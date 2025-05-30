/*
 * Copyright (C) 2025 Toshiaki Maki <makingx@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.cart.web;

import com.example.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "--spring.profiles.active=testcontainers", "--spring.sql.init.platform=postgresql" })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CartControllerIntegrationTest {

	RestClient restClient;

	String itemId;

	@BeforeEach
	void setUp(@LocalServerPort int port, @Autowired RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder.baseUrl("http://localhost:" + port)
			.defaultStatusHandler(__ -> true, (req, res) -> {
			})
			.build();
	}

	@Test
	@Order(1)
	void createCart() {
		ResponseEntity<JsonNode> response = this.restClient.get()
			.uri("/api/v1/carts?userId={userId}", "user123")
			.retrieve()
			.toEntity(JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		JsonNode body = response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.has("id")).isTrue();
		assertThat(body.get("id").asText()).isNotEmpty();
		assertThat(body.has("userId")).isTrue();
		assertThat(body.get("userId").asText()).isEqualTo("user123");
		assertThat(body.has("items")).isTrue();
		assertThat(body.get("items").isArray()).isTrue();
		assertThat(body.get("items").size()).isEqualTo(0);
		assertThat(body.has("totalAmount")).isTrue();
		assertThat(body.get("totalAmount").asDouble()).isEqualTo(0.0);
		assertThat(body.has("createdAt")).isTrue();
		assertThat(body.get("createdAt").asText()).isNotEmpty();
		assertThat(body.has("updatedAt")).isTrue();
		assertThat(body.get("updatedAt").asText()).isNotEmpty();
	}

	@Test
	@Order(2)
	void addItem1() {
		ResponseEntity<JsonNode> response = this.restClient.post()
			.uri("/api/v1/carts/items?userId={userId}", "user123")
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{
					    "productId": "product-001",
					    "productName": "iPhone 15",
					    "price": 999.99,
					    "quantity": 1
					}
					""")
			.retrieve()
			.toEntity(JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		JsonNode body = response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.has("id")).isTrue();
		assertThat(body.get("id").asText()).isNotEmpty();
		assertThat(body.has("userId")).isTrue();
		assertThat(body.get("userId").asText()).isEqualTo("user123");
		assertThat(body.has("items")).isTrue();
		assertThat(body.get("items").isArray()).isTrue();
		assertThat(body.get("items").size()).isEqualTo(1);
		JsonNode item = body.get("items").get(0);
		assertThat(item.has("id")).isTrue();
		assertThat(item.get("id").asText()).isNotEmpty();
		this.itemId = item.get("id").asText();
		assertThat(item.has("productId")).isTrue();
		assertThat(item.get("productId").asText()).isEqualTo("product-001");
		assertThat(item.has("productName")).isTrue();
		assertThat(item.get("productName").asText()).isEqualTo("iPhone 15");
		assertThat(item.has("price")).isTrue();
		assertThat(item.get("price").asDouble()).isEqualTo(999.99);
		assertThat(item.has("quantity")).isTrue();
		assertThat(item.get("quantity").asInt()).isEqualTo(1);
		assertThat(item.has("totalPrice")).isTrue();
		assertThat(item.get("totalPrice").asDouble()).isEqualTo(999.99);
		assertThat(item.has("createdAt")).isTrue();
		assertThat(item.get("createdAt").asText()).isNotEmpty();
		assertThat(item.has("updatedAt")).isTrue();
		assertThat(item.get("updatedAt").asText()).isNotEmpty();
		assertThat(body.has("totalAmount")).isTrue();
		assertThat(body.get("totalAmount").asDouble()).isEqualTo(999.99);
		assertThat(body.has("createdAt")).isTrue();
		assertThat(body.get("createdAt").asText()).isNotEmpty();
		assertThat(body.has("updatedAt")).isTrue();
		assertThat(body.get("updatedAt").asText()).isNotEmpty();
	}

	@Test
	@Order(3)
	void addItem2() {
		ResponseEntity<JsonNode> response = this.restClient.post()
			.uri("/api/v1/carts/items?userId={userId}", "user123")
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{
					    "productId": "product-002",
					    "productName": "MacBook Pro",
					    "price": 2499.99,
					    "quantity": 1
					  }
					""")
			.retrieve()
			.toEntity(JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		JsonNode body = response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.has("id")).isTrue();
		assertThat(body.get("id").asText()).isNotEmpty();
		assertThat(body.has("userId")).isTrue();
		assertThat(body.get("userId").asText()).isEqualTo("user123");
		assertThat(body.has("items")).isTrue();
		assertThat(body.get("items").isArray()).isTrue();
		assertThat(body.get("items").size()).isEqualTo(2);
		{
			JsonNode item = body.get("items").get(0);
			assertThat(item.has("id")).isTrue();
			assertThat(item.get("id").asText()).isNotEmpty();
			assertThat(item.has("productId")).isTrue();
			assertThat(item.get("productId").asText()).isEqualTo("product-001");
			assertThat(item.has("productName")).isTrue();
			assertThat(item.get("productName").asText()).isEqualTo("iPhone 15");
			assertThat(item.has("price")).isTrue();
			assertThat(item.get("price").asDouble()).isEqualTo(999.99);
			assertThat(item.has("quantity")).isTrue();
			assertThat(item.get("quantity").asInt()).isEqualTo(1);
			assertThat(item.has("totalPrice")).isTrue();
			assertThat(item.get("totalPrice").asDouble()).isEqualTo(999.99);
			assertThat(item.has("createdAt")).isTrue();
			assertThat(item.get("createdAt").asText()).isNotEmpty();
			assertThat(item.has("updatedAt")).isTrue();
			assertThat(item.get("updatedAt").asText()).isNotEmpty();
		}
		{
			JsonNode item = body.get("items").get(1);
			assertThat(item.has("id")).isTrue();
			assertThat(item.get("id").asText()).isNotEmpty();
			assertThat(item.has("productId")).isTrue();
			assertThat(item.get("productId").asText()).isEqualTo("product-002");
			assertThat(item.has("productName")).isTrue();
			assertThat(item.get("productName").asText()).isEqualTo("MacBook Pro");
			assertThat(item.has("price")).isTrue();
			assertThat(item.get("price").asDouble()).isEqualTo(2499.99);
			assertThat(item.has("quantity")).isTrue();
			assertThat(item.get("quantity").asInt()).isEqualTo(1);
			assertThat(item.has("totalPrice")).isTrue();
			assertThat(item.get("totalPrice").asDouble()).isEqualTo(2499.99);
			assertThat(item.has("createdAt")).isTrue();
			assertThat(item.get("createdAt").asText()).isNotEmpty();
			assertThat(item.has("updatedAt")).isTrue();
			assertThat(item.get("updatedAt").asText()).isNotEmpty();
		}
		assertThat(body.has("totalAmount")).isTrue();
		assertThat(body.get("totalAmount").asDouble()).isEqualTo(3499.98);
		assertThat(body.has("createdAt")).isTrue();
		assertThat(body.get("createdAt").asText()).isNotEmpty();
		assertThat(body.has("updatedAt")).isTrue();
		assertThat(body.get("updatedAt").asText()).isNotEmpty();
	}

	@Test
	@Order(4)
	void updateQuantity() {
		String itemId = Objects.requireNonNull(
				this.restClient.get().uri("/api/v1/carts?userId={userId}", "user123").retrieve().body(JsonNode.class))
			.get("items")
			.get(0)
			.get("id")
			.asText();
		ResponseEntity<JsonNode> response = this.restClient.patch()
			.uri("/api/v1/carts/items/{itemId}?userId={userId}", itemId, "user123")
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{"quantity": 3}
					""")
			.retrieve()
			.toEntity(JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		JsonNode body = response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.has("id")).isTrue();
		assertThat(body.get("id").asText()).isNotEmpty();
		assertThat(body.has("userId")).isTrue();
		assertThat(body.get("userId").asText()).isEqualTo("user123");
		assertThat(body.has("items")).isTrue();
		assertThat(body.get("items").isArray()).isTrue();
		assertThat(body.get("items").size()).isEqualTo(2);
		{
			JsonNode item = body.get("items").get(0);
			assertThat(item.has("id")).isTrue();
			assertThat(item.get("id").asText()).isNotEmpty();
			assertThat(item.has("productId")).isTrue();
			assertThat(item.get("productId").asText()).isEqualTo("product-001");
			assertThat(item.has("productName")).isTrue();
			assertThat(item.get("productName").asText()).isEqualTo("iPhone 15");
			assertThat(item.has("price")).isTrue();
			assertThat(item.get("price").asDouble()).isEqualTo(999.99);
			assertThat(item.has("quantity")).isTrue();
			assertThat(item.get("quantity").asInt()).isEqualTo(3);
			assertThat(item.has("totalPrice")).isTrue();
			assertThat(item.get("totalPrice").asDouble()).isEqualTo(2999.97);
			assertThat(item.has("createdAt")).isTrue();
			assertThat(item.get("createdAt").asText()).isNotEmpty();
			assertThat(item.has("updatedAt")).isTrue();
			assertThat(item.get("updatedAt").asText()).isNotEmpty();
		}
		{
			JsonNode item = body.get("items").get(1);
			assertThat(item.has("id")).isTrue();
			assertThat(item.get("id").asText()).isNotEmpty();
			assertThat(item.has("productId")).isTrue();
			assertThat(item.get("productId").asText()).isEqualTo("product-002");
			assertThat(item.has("productName")).isTrue();
			assertThat(item.get("productName").asText()).isEqualTo("MacBook Pro");
			assertThat(item.has("price")).isTrue();
			assertThat(item.get("price").asDouble()).isEqualTo(2499.99);
			assertThat(item.has("quantity")).isTrue();
			assertThat(item.get("quantity").asInt()).isEqualTo(1);
			assertThat(item.has("totalPrice")).isTrue();
			assertThat(item.get("totalPrice").asDouble()).isEqualTo(2499.99);
			assertThat(item.has("createdAt")).isTrue();
			assertThat(item.get("createdAt").asText()).isNotEmpty();
			assertThat(item.has("updatedAt")).isTrue();
			assertThat(item.get("updatedAt").asText()).isNotEmpty();
		}
		assertThat(body.has("totalAmount")).isTrue();
		assertThat(body.get("totalAmount").asDouble()).isEqualTo(5499.96);
		assertThat(body.has("createdAt")).isTrue();
		assertThat(body.get("createdAt").asText()).isNotEmpty();
		assertThat(body.has("updatedAt")).isTrue();
		assertThat(body.get("updatedAt").asText()).isNotEmpty();
	}

	@Test
	@Order(5)
	void removeItem() {
		String itemId = Objects.requireNonNull(
				this.restClient.get().uri("/api/v1/carts?userId={userId}", "user123").retrieve().body(JsonNode.class))
			.get("items")
			.get(0)
			.get("id")
			.asText();
		ResponseEntity<JsonNode> response = this.restClient.delete()
			.uri("/api/v1/carts/items/{itemId}?userId={userId}", itemId, "user123")
			.retrieve()
			.toEntity(JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		JsonNode body = response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.has("id")).isTrue();
		assertThat(body.get("id").asText()).isNotEmpty();
		assertThat(body.has("userId")).isTrue();
		assertThat(body.get("userId").asText()).isEqualTo("user123");
		assertThat(body.has("items")).isTrue();
		assertThat(body.get("items").isArray()).isTrue();
		assertThat(body.get("items").size()).isEqualTo(1);
		{
			JsonNode item = body.get("items").get(0);
			assertThat(item.has("id")).isTrue();
			assertThat(item.get("id").asText()).isNotEmpty();
			assertThat(item.has("productId")).isTrue();
			assertThat(item.get("productId").asText()).isEqualTo("product-002");
			assertThat(item.has("productName")).isTrue();
			assertThat(item.get("productName").asText()).isEqualTo("MacBook Pro");
			assertThat(item.has("price")).isTrue();
			assertThat(item.get("price").asDouble()).isEqualTo(2499.99);
			assertThat(item.has("quantity")).isTrue();
			assertThat(item.get("quantity").asInt()).isEqualTo(1);
			assertThat(item.has("totalPrice")).isTrue();
			assertThat(item.get("totalPrice").asDouble()).isEqualTo(2499.99);
			assertThat(item.has("createdAt")).isTrue();
			assertThat(item.get("createdAt").asText()).isNotEmpty();
			assertThat(item.has("updatedAt")).isTrue();
			assertThat(item.get("updatedAt").asText()).isNotEmpty();
		}
		assertThat(body.has("totalAmount")).isTrue();
		assertThat(body.get("totalAmount").asDouble()).isEqualTo(2499.99);
		assertThat(body.has("createdAt")).isTrue();
		assertThat(body.get("createdAt").asText()).isNotEmpty();
		assertThat(body.has("updatedAt")).isTrue();
		assertThat(body.get("updatedAt").asText()).isNotEmpty();
	}

	@Test
	@Order(6)
	void deleteCart() {
		ResponseEntity<JsonNode> response = this.restClient.delete()
			.uri("/api/v1/carts?userId={userId}", "user123")
			.retrieve()
			.toEntity(JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		JsonNode body = response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.has("message")).isTrue();
		assertThat(body.get("message").asText()).isEqualTo("Cart deleted successfully");
	}

}