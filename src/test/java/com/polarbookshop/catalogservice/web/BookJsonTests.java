package com.polarbookshop.catalogservice.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.polarbookshop.catalogservice.domain.Book;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
public class BookJsonTests {

  @Autowired
  private JacksonTester<Book> json;

  @Test
  void testSerialize() throws IOException {
    var now = Instant.now();
    var book = new Book(394L, "1234567890", "Title", "Author", 9.90, now, now, 21);
    var jsonContent = json.write(book);
    assertThat(jsonContent).extractingJsonPathNumberValue("@.id")
        .isEqualTo(book.id().intValue());
    assertThat(jsonContent).extractingJsonPathStringValue("@.isbn")
        .isEqualTo(book.isbn());
    assertThat(jsonContent).extractingJsonPathStringValue("@.title")
        .isEqualTo(book.title());
    assertThat(jsonContent).extractingJsonPathStringValue("@.author")
        .isEqualTo(book.author());
    assertThat(jsonContent).extractingJsonPathNumberValue("@.price")
        .isEqualTo(book.price());
    assertThat(jsonContent).extractingJsonPathNumberValue("@.version")
        .isEqualTo(book.version());
  }

  @Test
  void testDeserialize() throws IOException {
    var instant = Instant.parse("2023-05-07T04:42:25.276130Z");
    var content = """
        {
          "id": 394,
          "isbn": "1234567890",
          "title": "Title",
          "author": "Author",
          "price": 9.90,
          "version": 21,
          "createdDate": "2023-05-07T04:42:25.276130Z",
          "lastModifiedDate": "2023-05-07T04:42:25.276130Z"
        }
        """;
    assertThat(json.parse(content))
        .usingRecursiveComparison()
        .isEqualTo(new Book(394L, "1234567890", "Title", "Author", 9.90, instant, instant, 21));
  }
}
