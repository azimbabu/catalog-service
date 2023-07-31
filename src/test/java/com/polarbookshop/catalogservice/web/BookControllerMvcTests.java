package com.polarbookshop.catalogservice.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarbookshop.catalogservice.config.SecurityConfig;
import com.polarbookshop.catalogservice.domain.Book;
import com.polarbookshop.catalogservice.domain.BookNotFoundException;
import com.polarbookshop.catalogservice.domain.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(BookController.class)
@Import(SecurityConfig.class)
public class BookControllerMvcTests {

  private static final String ROLE_EMPLOYEE = "ROLE_employee";
  private static final String ROLE_CUSTOMER = "ROLE_customer";

  @Autowired
  MockMvc mockMvc;

  @Autowired
  ObjectMapper objectMapper;

  @MockBean
  BookService bookService;

  @MockBean
  JwtDecoder jwtDecoder;

  @Test
  void whenGetBookExistingAndAuthenticatedThenShouldReturn200() throws Exception {
    var isbn = "7373731394";
    var expectedBook = Book.of(isbn, "Title", "Author", 9.90, "Polarsophia");
    given(bookService.viewBookDetails(isbn)).willReturn(expectedBook);
    mockMvc.perform(get("/books/" + isbn).with(SecurityMockMvcRequestPostProcessors.jwt()))
        .andExpect(status().isOk());
  }

  @Test
  void whenGetBookExistingAndNotAuthenticatedThenShouldReturn200() throws Exception {
    var isbn = "7373731394";
    var expectedBook = Book.of(isbn, "Title", "Author", 9.90, "Polarsophia");
    given(bookService.viewBookDetails(isbn)).willReturn(expectedBook);
    mockMvc.perform(get("/books/" + isbn))
        .andExpect(status().isOk());
  }

  @Test
  void whenGetBookNotExistingAndAuthenticatedThenShouldReturn404() throws Exception {
    var isbn = "7373731394";
    given(bookService.viewBookDetails(isbn)).willThrow(BookNotFoundException.class);
    mockMvc.perform(get("/books/" + isbn).with(SecurityMockMvcRequestPostProcessors.jwt()))
        .andExpect(status().isNotFound());
  }

  @Test
  void whenGetBookNotExistingAndNotAuthenticatedThenShouldReturn404() throws Exception {
    var isbn = "7373731394";
    given(bookService.viewBookDetails(isbn)).willThrow(BookNotFoundException.class);
    mockMvc.perform(get("/books/" + isbn))
        .andExpect(status().isNotFound());
  }

  @Test
  void whenDeleteBookWithEmployeeRoleThenShouldReturn204() throws Exception {
    var isbn = "7373731394";
    mockMvc.perform(MockMvcRequestBuilders.delete("/books/" + isbn)
            .with(SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_employee"))))
        .andExpect(MockMvcResultMatchers.status().isNoContent());
  }

  @Test
  void whenDeleteBookWithCustomerRoleThenShouldReturn403() throws Exception {
    var isbn = "7373731394";
    mockMvc.perform(MockMvcRequestBuilders.delete("/books/" + isbn)
            .with(SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_customer"))))
        .andExpect(MockMvcResultMatchers.status().isForbidden());
  }

  @Test
  void whenDeleteBookNotAuthenticatedThenShouldReturn401() throws Exception {
    var isbn = "7373731394";
    mockMvc.perform(MockMvcRequestBuilders.delete("/books/" + isbn))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  void whenPostBookWithEmployeeRoleThenShouldReturn201() throws Exception {
    var isbn = "7373731394";
    var bookToCreate = Book.of(isbn, "Title", "Author", 9.90, "Polarsophia");
    given(bookService.addBookToCatalog(bookToCreate)).willReturn(bookToCreate);
    mockMvc.perform(post("/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bookToCreate))
            .with(SecurityMockMvcRequestPostProcessors.jwt().authorities(new SimpleGrantedAuthority(
                ROLE_EMPLOYEE))))
        .andExpect(status().isCreated());
  }

  @Test
  void whenPostBookWithCustomerRoleThenShouldReturn403() throws Exception {
    var isbn = "7373731394";
    var bookToCreate = Book.of(isbn, "Title", "Author", 9.90, "Polarsophia");
    given(bookService.addBookToCatalog(bookToCreate)).willReturn(bookToCreate);
    mockMvc.perform(post("/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bookToCreate))
            .with(SecurityMockMvcRequestPostProcessors.jwt().authorities(new SimpleGrantedAuthority(
                ROLE_CUSTOMER))))
        .andExpect(status().isForbidden());
  }

  @Test
  void whenPostBookAndNotAuthenticatedThenShouldReturn403() throws Exception {
    var isbn = "7373731394";
    var bookToCreate = Book.of(isbn, "Title", "Author", 9.90, "Polarsophia");
    given(bookService.addBookToCatalog(bookToCreate)).willReturn(bookToCreate);
    mockMvc.perform(post("/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bookToCreate)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void whenPutBookWithEmployeeRoleThenShouldReturn200() throws Exception {
    var isbn = "7373731394";
    var bookToUpdate = Book.of(isbn, "Title", "Author", 9.90, "Polarsophia");
    given(bookService.editBookDetails(isbn, bookToUpdate)).willReturn(bookToUpdate);
    mockMvc.perform(put("/books/" + isbn)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bookToUpdate))
            .with(SecurityMockMvcRequestPostProcessors.jwt().authorities(new SimpleGrantedAuthority(
                ROLE_EMPLOYEE))))
        .andExpect(status().isOk());
  }

  @Test
  void whenPutBookWithCustomerRoleThenShouldReturn403() throws Exception {
    var isbn = "7373731394";
    var bookToUpdate = Book.of(isbn, "Title", "Author", 9.90, "Polarsophia");
    given(bookService.editBookDetails(isbn, bookToUpdate)).willReturn(bookToUpdate);
    mockMvc.perform(put("/books/" + isbn)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bookToUpdate))
            .with(SecurityMockMvcRequestPostProcessors.jwt().authorities(new SimpleGrantedAuthority(
                ROLE_CUSTOMER))))
        .andExpect(status().isForbidden());
  }

  @Test
  void whenPutBookAndNotAuthenticatedThenShouldReturn401() throws Exception {
    var isbn = "7373731394";
    var bookToUpdate = Book.of(isbn, "Title", "Author", 9.90, "Polarsophia");
    given(bookService.editBookDetails(isbn, bookToUpdate)).willReturn(bookToUpdate);
    mockMvc.perform(put("/books/" + isbn)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bookToUpdate)))
        .andExpect(status().isUnauthorized());
  }
}
