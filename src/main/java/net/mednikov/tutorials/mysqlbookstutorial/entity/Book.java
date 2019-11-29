package net.mednikov.tutorials.mysqlbookstutorial.entity;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class Book{

    @Getter private UUID id;
    @Getter private String title;
    @Getter private String author;
    @Getter private int year;
    
}