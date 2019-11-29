package net.mednikov.tutorials.mysqlbookstutorial.mapper;

import java.util.UUID;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import net.mednikov.tutorials.mysqlbookstutorial.entity.Book;

public class BookMapper{

    public static Book from (Row row){
        UUID id = UUID.fromString(row.getString("book_id"));
        String title = row.getString("book_title");
        String author = row.getString("book_author");
        int year = row.getInteger("book_year");
        return new Book(id, title, author, year);
    }

    public static Tuple to (Book book){
        return Tuple.of(book.getId(), book.getTitle(), book.getAuthor(), book.getYear());
    }
}