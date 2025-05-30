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

import com.example.cart.Cart;
import com.example.cart.CartItem;
import com.example.cart.CartService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/carts")
public class CartController {

	private final CartService cartService;

	public CartController(CartService cartService) {
		this.cartService = cartService;
	}

	@GetMapping
	public ResponseEntity<CartResponse> getCart(@RequestParam String userId) {
		Cart cart = this.cartService.getOrCreateCart(userId);
		return ResponseEntity.ok(new CartResponse(cart));
	}

	@GetMapping("/{cartId}")
	public ResponseEntity<CartResponse> getCartById(@PathVariable UUID cartId) {
		Cart cart = this.cartService.getCartById(cartId);
		return ResponseEntity.ok(new CartResponse(cart));
	}

	@PostMapping("/items")
	public ResponseEntity<CartResponse> addToCart(@RequestParam String userId,
			@RequestBody CartService.AddToCartRequest request) {
		Cart cart = this.cartService.addToCart(userId, request);
		return ResponseEntity.ok(new CartResponse(cart));
	}

	@PatchMapping("/items/{itemId}")
	public ResponseEntity<CartResponse> updateItemQuantity(@PathVariable UUID itemId, @RequestParam String userId,
			@RequestBody UpdateQuantityRequest request) {
		Cart cart = this.cartService.updateItemQuantity(userId, itemId, request.quantity());
		return ResponseEntity.ok(new CartResponse(cart));
	}

	@DeleteMapping("/items/{itemId}")
	public ResponseEntity<CartResponse> removeItemFromCart(@PathVariable UUID itemId, @RequestParam String userId) {
		Cart cart = this.cartService.removeItemFromCart(userId, itemId);
		return ResponseEntity.ok(new CartResponse(cart));
	}

	@DeleteMapping("/items")
	public ResponseEntity<Map<String, String>> clearCart(@RequestParam String userId) {
		this.cartService.clearCart(userId);
		return ResponseEntity.ok(Map.of("message", "Cart cleared successfully"));
	}

	@DeleteMapping
	public ResponseEntity<Map<String, String>> deleteCart(@RequestParam String userId) {
		this.cartService.deleteCart(userId);
		return ResponseEntity.ok(Map.of("message", "Cart deleted successfully"));
	}

	public record CartItemResponse(UUID id, String productId, String productName, BigDecimal price, Integer quantity,
			BigDecimal totalPrice, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
		public CartItemResponse(CartItem cartItem) {
			this(cartItem.getId(), cartItem.getProductId(), cartItem.getProductName(), cartItem.getPrice(),
					cartItem.getQuantity(), cartItem.getTotalPrice(), cartItem.getCreatedAt(), cartItem.getUpdatedAt());
		}
	}

	public record CartResponse(UUID id, String userId, List<CartItemResponse> items, BigDecimal totalAmount,
			OffsetDateTime createdAt, OffsetDateTime updatedAt) {
		public CartResponse(Cart cart) {
			this(cart.getId(), cart.getUserId(), cart.getItems().stream().map(CartItemResponse::new).toList(),
					cart.getItems().stream().map(CartItem::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add),
					cart.getCreatedAt(), cart.getUpdatedAt());
		}
	}

	public record UpdateQuantityRequest(Integer quantity) {
	}

}
