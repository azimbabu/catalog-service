package com.polarbookshop.catalogservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.polarbookshop.catalogservice.domain.Book;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@Testcontainers
class CatalogServiceApplicationTests {

  @Container
  private static final KeycloakContainer keycloakContainer = new KeycloakContainer(
      "quay.io/keycloak/keycloak:19.0")
      .withRealmImportFile("test-realm-config.json");

  @Autowired
  private WebTestClient webTestClient;
  // Customer
  private static KeycloakToken bjornTokens;
  // Customer and employee
  private static KeycloakToken isabelleTokens;

  @DynamicPropertySource
  static void dynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
        () -> keycloakContainer.getAuthServerUrl() + "realms/PolarBookshop");
  }

  @BeforeAll
  static void generateAccessTokens() {
    WebClient webClient = WebClient.builder()
        .baseUrl(keycloakContainer.getAuthServerUrl()
            + "realms/PolarBookshop/protocol/openid-connect/token")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .build();

    isabelleTokens = authenticateWith(webClient, "isabelle", "password");
    bjornTokens = authenticateWith(webClient, "bjorn", "password");
  }

  private static KeycloakToken authenticateWith(WebClient webClient, String username,
      String password) {
    return webClient.post()
        .body(BodyInserters.fromFormData("grant_type", "password")
            .with("client_id", "polar-test")
            .with("username", username)
            .with("password", password)
        ).retrieve()
        .bodyToMono(KeycloakToken.class)
        .block();
  }

  @Test
  void whenGetRequestWithIdThenBookReturned() {
    var isbn = "1231231230";
    var bookToCreate = Book.of(isbn, "Title", "Author", 9.90, "Polarsophia");
    Book expectedBook = webTestClient
        .post()
        .uri("/books")
        .headers(headers -> headers.setBearerAuth(isabelleTokens.accessToken()))
        .bodyValue(bookToCreate)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(Book.class).value(book -> assertThat(book).isNotNull())
        .returnResult().getResponseBody();

    webTestClient
        .get()
        .uri("/books/" + isbn)
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(Book.class).value(actualBook -> {
          assertThat(actualBook).isNotNull();
          assertThat(actualBook.isbn()).isEqualTo(expectedBook.isbn());
        });

  }

  @Test
  void whenPostRequestThenBookCreated() {
    var expectedBook = Book.of("1231231231", "Title", "Author", 9.90, "Polarsophia");
    webTestClient
        .post()
        .uri("/books")
        .headers(headers -> headers.setBearerAuth(isabelleTokens.accessToken()))
        .bodyValue(expectedBook)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(Book.class).value(actualBook -> {
          assertThat(actualBook).isNotNull();
          assertThat(actualBook.isbn()).isEqualTo(expectedBook.isbn());
        });
  }

  @Test
  void whenPostRequestUnauthorizedThen403() {
    var expectedBook = Book.of("1231231231", "Title", "Author", 9.90, "Polarsophia");
    webTestClient
        .post()
        .uri("/books")
        .headers(headers -> headers.setBearerAuth(bjornTokens.accessToken()))
        .bodyValue(expectedBook)
        .exchange()
        .expectStatus().isForbidden();
  }

  @Test
  void whenPostRequestUnauthenticatedThen401() {
    var expectedBook = Book.of("1231231231", "Title", "Author", 9.90, "Polarsophia");
    webTestClient
        .post()
        .uri("/books")
        .bodyValue(expectedBook)
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  void whenPutRequestThenBookUpdated() {
    var isbn = "1231231232";
    var bookToCreate = Book.of(isbn, "Title", "Author", 9.90, "Polarsophia");

    Book createdBook = webTestClient
        .post()
        .uri("/books")
        .headers(headers -> headers.setBearerAuth(isabelleTokens.accessToken()))
        .bodyValue(bookToCreate)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(Book.class).value(book -> assertThat(book).isNotNull())
        .returnResult().getResponseBody();

    var bookToUpdate = new Book(createdBook.id(), createdBook.isbn(), createdBook.title(),
        createdBook.author(),
        7.75, createdBook.publisher(), createdBook.createdDate(), createdBook.lastModifiedDate(),
        createdBook.createdBy(), createdBook.lastModifiedBy(), createdBook.version());

    webTestClient
        .put()
        .uri("/books/" + isbn)
        .headers(headers -> headers.setBearerAuth(isabelleTokens.accessToken()))
        .bodyValue(bookToUpdate)
        .exchange()
        .expectStatus().isOk()
        .expectBody(Book.class).value(actualBook -> {
          assertThat(actualBook).isNotNull();
          assertThat(actualBook.price()).isEqualTo(bookToUpdate.price());
        });
  }

  @Test
  void whenDeleteRequestThenBookDeleted() {
    var isbn = "1231231233";
    var bookToCreate = Book.of(isbn, "Title", "Author", 9.90, "Polarsophia");

    webTestClient
        .post()
        .uri("/books")
        .headers(headers -> headers.setBearerAuth(isabelleTokens.accessToken()))
        .bodyValue(bookToCreate)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(Book.class).value(book -> assertThat(book).isNotNull());

    webTestClient
        .delete()
        .uri("/books/" + isbn)
        .headers(headers -> headers.setBearerAuth(isabelleTokens.accessToken()))
        .exchange()
        .expectStatus().isNoContent();

    webTestClient
        .get()
        .uri("/books/" + isbn)
        .exchange()
        .expectStatus().isNotFound()
        .expectBody(String.class).value(errorMessage -> assertThat(errorMessage).isEqualTo(
            "The book with ISBN " + isbn + " was not found."));
  }

  private record KeycloakToken(String accessToken) {

    @JsonCreator
    private KeycloakToken(@JsonProperty("access_token") final String accessToken) {
      this.accessToken = accessToken;
    }
  }

}
