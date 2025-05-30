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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Cart {

	private final UUID id;

	private final String userId;

	private final OffsetDateTime createdAt;

	private OffsetDateTime updatedAt;

	private final List<CartItem> items = new ArrayList<>();

	public Cart(UUID id, String userId, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
		this.id = id;
		this.userId = userId;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public void addItem(String productId, String productName, BigDecimal price, Integer quantity) {
		Optional<CartItem> existingItem = findItemByProductId(productId);

		if (existingItem.isPresent()) {
			// Update quantity if item exists
			CartItem item = existingItem.get();
			item.setQuantity(item.getQuantity() + quantity);
		}
		else {
			// Add new item
			CartItem newItem = new CartItem(this.id, productId, productName, price, quantity);
			this.items.add(newItem);
		}
	}

	public void addItem(CartItem item) {
		this.items.add(item);
	}

	public void updateItemQuantity(UUID itemId, Integer quantity) {
		CartItem item = findItemById(itemId)
			.orElseThrow(() -> new IllegalArgumentException("Cart item not found with id: " + itemId));
		item.setQuantity(quantity);
	}

	public void removeItem(UUID itemId) {
		this.items.removeIf(item -> item.getId().equals(itemId));
	}

	public void clearItems() {
		this.items.clear();
	}

	public BigDecimal getTotalAmount() {
		return items.stream().map(CartItem::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public int getItemCount() {
		return items.stream().mapToInt(CartItem::getQuantity).sum();
	}

	public boolean isEmpty() {
		return items.isEmpty();
	}

	public boolean belongsToUser(String userId) {
		return this.userId.equals(userId);
	}

	private Optional<CartItem> findItemByProductId(String productId) {
		return items.stream().filter(item -> item.getProductId().equals(productId)).findFirst();
	}

	private Optional<CartItem> findItemById(UUID itemId) {
		return items.stream().filter(item -> item.getId().equals(itemId)).findFirst();
	}

	public UUID getId() {
		return id;
	}

	public String getUserId() {
		return userId;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public List<CartItem> getItems() {
		return Collections.unmodifiableList(items); // Return defensive copy
	}

	@Override
	public String toString() {
		return "Cart{" + "id=" + id + ", userId='" + userId + '\'' + ", createdAt=" + createdAt + ", updatedAt="
				+ updatedAt + ", itemCount=" + items.size() + ", totalAmount=" + getTotalAmount() + '}';
	}

}
