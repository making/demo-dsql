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
package com.example.cart;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 4,
		backoff = @Backoff(delay = 100, multiplier = 2, random = true))
public class CartService {

	private final CartRepository cartRepository;

	public CartService(CartRepository cartRepository) {
		this.cartRepository = cartRepository;
	}

	public Cart getOrCreateCart(String userId) {
		return cartRepository.findByUserId(userId).orElseGet(() -> cartRepository.create(userId));
	}

	public Cart getCartById(UUID cartId) {
		return cartRepository.findById(cartId)
			.orElseThrow(() -> new IllegalArgumentException("Cart not found with id: " + cartId));
	}

	public Cart addToCart(String userId, AddToCartRequest request) {
		// Validate request
		validateAddToCartRequest(request);

		// Get or create cart
		Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> cartRepository.create(userId));

		// Add item to cart using aggregate method
		cart.addItem(request.productId(), request.productName(), request.price(), request.quantity());

		// Save the entire aggregate
		cartRepository.save(cart);

		// Return updated cart
		return cart;
	}

	public Cart updateItemQuantity(String userId, UUID itemId, Integer quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be greater than 0");
		}

		Cart cart = findCartByUserId(userId);

		// Verify cart belongs to user
		if (!cart.belongsToUser(userId)) {
			throw new IllegalStateException("Cart does not belong to user");
		}

		// Update item quantity using aggregate method
		cart.updateItemQuantity(itemId, quantity);

		// Save the entire aggregate
		cartRepository.save(cart);

		return cart;
	}

	public Cart removeItemFromCart(String userId, UUID itemId) {
		Cart cart = findCartByUserId(userId);

		// Verify cart belongs to user
		if (!cart.belongsToUser(userId)) {
			throw new RuntimeException("Cart does not belong to user");
		}

		// Remove item using aggregate method
		cart.removeItem(itemId);

		// Save the entire aggregate
		cartRepository.save(cart);

		return cart;
	}

	public void clearCart(String userId) {
		Cart cart = findCartByUserId(userId);

		// Clear all items using aggregate method
		cart.clearItems();

		// Save the entire aggregate
		cartRepository.save(cart);
	}

	public void deleteCart(String userId) {
		Cart cart = findCartByUserId(userId);
		cartRepository.deleteById(cart.getId());
	}

	private void validateAddToCartRequest(AddToCartRequest request) {
		if (request.quantity() == null || request.quantity() <= 0) {
			throw new IllegalArgumentException("Quantity must be greater than 0");
		}
		if (request.price() == null || request.price().compareTo(java.math.BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Price must be greater than 0");
		}
		if (request.productId() == null || request.productId().trim().isEmpty()) {
			throw new IllegalArgumentException("Product ID is required");
		}
		if (request.productName() == null || request.productName().trim().isEmpty()) {
			throw new IllegalArgumentException("Product name is required");
		}
	}

	private Cart findCartByUserId(String userId) {
		return cartRepository.findByUserId(userId)
			.orElseThrow(() -> new IllegalArgumentException("Cart not found for user: " + userId));
	}

	public record AddToCartRequest(String productId, String productName, BigDecimal price, Integer quantity) {
	}

}
