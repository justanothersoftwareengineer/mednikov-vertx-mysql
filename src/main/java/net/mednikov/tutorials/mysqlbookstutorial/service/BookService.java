package net.mednikov.tutorials.mysqlbookstutorial.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.mednikov.tutorials.mysqlbookstutorial.entity.Book;
import net.mednikov.tutorials.mysqlbookstutorial.mapper.BookMapper;

public class BookService extends AbstractVerticle {

    private final String DB_URI = "mysql://user:secret@localhost:3306/booksdb";
    private MySQLPool pool;

    @Override
    public void start(Promise<Void> promise) throws Exception {
        pool = MySQLPool.pool(vertx, DB_URI);
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route("/books/*").handler(BodyHandler.create());
        router.get("/books/one/:id").handler(this::getOneBook);
        router.get("/books/all").handler(this::getAllBooks);
        router.post("/books/new").handler(this::addBook);
        router.delete("/books/:id").handler(this::deleteBook);
        server.requestHandler(router);
        server.listen(4567, res->{
            if (res.succeeded()){
                System.out.println("App started");
                promise.complete();
            } else {
                System.out.println(res.cause().getLocalizedMessage());
                promise.fail(res.cause());
            }
        });
    }

    private void getOneBook(RoutingContext context){
        UUID id = UUID.fromString(context.pathParam("id"));
        pool.getConnection(res1->{
            if (res1.succeeded()){
                SqlConnection con = res1.result();
                con.preparedQuery("SELECT book_id, book_title, book_author, book_year FROM books where book_id=?", Tuple.of(id), res2->{
                    if (res2.succeeded()){
                        RowSet<Row> results = res2.result();
                        if (results.size()==0){
                            con.close();
                            context.response().setStatusCode(404).end("Not found");
                        }
                        results.forEach(row->{
                            Book book = BookMapper.from(row);
                            con.close();
                            context.response().setStatusCode(200).end(JsonObject.mapFrom(book).encode());
                        });
                    } else {
                        con.close();
                        System.out.println(res1.cause().getMessage());
                        context.response().setStatusCode(500).end();
                    }
                });
            } else {
                System.out.println(res1.cause().getMessage());
                context.response().setStatusCode(500).end();
            }
        });
    }

    private void addBook(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        UUID id = UUID.fromString(body.getString("id"));
        String title = body.getString("title");
        String author = body.getString("author");
        int year = body.getInteger("year");
        Book book = new Book(id, title, author, year);
        Tuple data = BookMapper.to(book);
        pool.getConnection(res1->{
            if (res1.succeeded()){
                SqlConnection con = res1.result();
                con.preparedQuery("INSERT INTO books (book_id, book_title, book_author, book_year) VALUES (?,?,?,?)", data, res2->{
                    if (res2.succeeded()){
                        con.close();
                        context.response().setStatusCode(200).end("Success");
                    } else {
                        con.close();
                        System.out.println(res2.cause().getMessage());
                        context.response().setStatusCode(500).end(res2.cause().getMessage());
                    }
                });
            } else {
               System.out.println(res1.cause().getMessage());
               context.response().setStatusCode(500).end(); 
            }
        });
    }

    private void deleteBook(RoutingContext context){
        UUID id = UUID.fromString(context.pathParam("id"));
        pool.getConnection(res1->{
            if (res1.succeeded()){
                SqlConnection con = res1.result();
                con.preparedQuery("DELETE FROM books WHERE book_id=?", Tuple.of(id), res2->{
                    if (res2.succeeded()){
                        con.close();
                        context.response().setStatusCode(200).end("Success");
                    } else {
                        con.close();
                        System.out.println(res2.cause().getMessage());
                        context.response().setStatusCode(500).end(); 
                    }
                });
            } else {
                System.out.println(res1.cause().getMessage());
                context.response().setStatusCode(500).end(); 
            }
        });
    }

    private void getAllBooks(RoutingContext context){
        pool.getConnection(res1->{
            if (res1.succeeded()){
                SqlConnection con = res1.result();
                con.query("SELECT book_id, book_title, book_author, book_year FROM books", res2->{
                    if (res2.succeeded()){
                        List<Book> books = new ArrayList<>();
                        RowSet<Row> rows = res2.result();
                        rows.forEach(row->{
                            Book book = BookMapper.from(row);
                            books.add(book);
                        });
                        JsonArray result = new JsonArray(books);
                        context.response().setStatusCode(200).end(result.encode());
                    } else {
                        con.close();
                        System.out.println(res2.cause().getMessage());
                        context.response().setStatusCode(500).end(); 
                    }
                });
            } else {
                System.out.println(res1.cause().getMessage());
                context.response().setStatusCode(500).end(); 
            }
        });
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new BookService());
    }
}