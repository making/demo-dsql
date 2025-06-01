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

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RegisterReflectionForBinding(Cart.class)
public class CartRepository {

	private final JdbcClient jdbcClient;

	private final Clock clock;

	public CartRepository(JdbcClient jdbcClient, Clock clock) {
		this.jdbcClient = jdbcClient;
		this.clock = clock;
	}

	@Transactional
	public Cart create(String userId) {
		String sql = """
				INSERT INTO carts (user_id, created_at, updated_at)
				VALUES (?, ?, ?)
				RETURNING id
				""";

		OffsetDateTime now = OffsetDateTime.now(this.clock);

		UUID id = jdbcClient.sql(sql).param(userId).param(now).param(now).query(UUID.class).single();

		return new Cart(id, userId, now, now);
	}

	public Optional<Cart> findById(UUID id) {
		String cartSql = """
				SELECT id, user_id, created_at, updated_at
				FROM carts
				WHERE id = ?
				""";

		Optional<Cart> cart = jdbcClient.sql(cartSql)
			.param(id)
			.query((rs, rowNum) -> new Cart(UUID.fromString(rs.getString("id")), rs.getString("user_id"),
					rs.getObject("created_at", OffsetDateTime.class), rs.getObject("updated_at", OffsetDateTime.class)))
			.optional();

		if (cart.isEmpty()) {
			return Optional.empty();
		}

		// Load cart items
		List<CartItem> items = findCartItems(id);
		Cart cartWithItems = cart.get();
		items.forEach(cartWithItems::addItem);

		return Optional.of(cartWithItems);
	}

	public Optional<Cart> findByUserId(String userId) {
		String cartSql = """
				SELECT id, user_id, created_at, updated_at
				FROM carts
				WHERE user_id = ?
				ORDER BY created_at DESC
				LIMIT 1
				""";

		Optional<Cart> cart = jdbcClient.sql(cartSql).param(userId).query(Cart.class).optional();

		if (cart.isEmpty()) {
			return Optional.empty();
		}

		// Load cart items
		List<CartItem> items = findCartItems(cart.get().getId());
		Cart cartWithItems = cart.get();
		items.forEach(cartWithItems::addItem);

		return Optional.of(cartWithItems);
	}

	@Transactional
	public void save(Cart cart) {
		// Verify cart exists before saving items
		if (!cartExists(cart.getId())) {
			throw new IllegalStateException("Cannot save items for non-existent cart: " + cart.getId());
		}
		boolean updated = false;
		// Get existing cart items from DB
		List<CartItem> existingItems = findCartItems(cart.getId());
		List<CartItem> currentItems = cart.getItems();
		// Create maps for efficient lookup
		Map<UUID, CartItem> existingItemsMap = existingItems.stream()
			.collect(java.util.stream.Collectors.toMap(CartItem::getId, item -> item));
		// Process current items: UPDATE existing items or INSERT new items
		for (CartItem currentItem : currentItems) {
			if (currentItem.getId() == null) {
				// INSERT new item
				insertCartItem(cart.getId(), currentItem);
				updated = true;
			}
			else {
				CartItem existingItem = existingItemsMap.get(currentItem.getId());
				if (!Objects.equals(currentItem, existingItem)) {
					// UPDATE existing item
					updateCartItem(currentItem);
					updated = true;
				}
			}
		}
		// DELETE items that exist in DB but not in current items
		Set<UUID> currentItemIds = currentItems.stream()
			.map(CartItem::getId)
			.filter(java.util.Objects::nonNull)
			.collect(java.util.stream.Collectors.toSet());
		List<UUID> itemsToDelete = existingItems.stream()
			.map(CartItem::getId)
			.filter(id -> !currentItemIds.contains(id))
			.collect(java.util.stream.Collectors.toList());
		if (!itemsToDelete.isEmpty()) {
			deleteCartItems(itemsToDelete);
			updated = true;
		}
		if (updated) {
			// Update cart timestamp
			String updateCartSql = """
					UPDATE carts
					SET updated_at = ?
					WHERE id = ?
					""";
			OffsetDateTime updatedAt = OffsetDateTime.now(this.clock);
			jdbcClient.sql(updateCartSql).param(updatedAt).param(cart.getId()).update();
			cart.setUpdatedAt(updatedAt);
		}
	}

	@Transactional
	public void deleteById(UUID id) {
		// Manually cascade delete: Delete cart items first, then cart
		String deleteItemsSql = "DELETE FROM cart_items WHERE cart_id = ?";
		jdbcClient.sql(deleteItemsSql).param(id).update();
		String deleteCartSql = "DELETE FROM carts WHERE id = ?";
		jdbcClient.sql(deleteCartSql).param(id).update();
	}

	private List<CartItem> findCartItems(UUID cartId) {
		String sql = """
				SELECT id, cart_id, product_id, product_name, price, quantity, created_at, updated_at
				FROM cart_items
				WHERE cart_id = ?
				ORDER BY created_at ASC
				""";
		return jdbcClient.sql(sql).param(cartId).query(CartItem.class).list();
	}

	private void insertCartItem(UUID cartId, CartItem item) {
		String sql = """
				INSERT INTO cart_items (cart_id, product_id, product_name, price, quantity, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				RETURNING id
				""";
		OffsetDateTime now = OffsetDateTime.now(this.clock);
		UUID id = jdbcClient.sql(sql)
			.param(cartId)
			.param(item.getProductId())
			.param(item.getProductName())
			.param(item.getPrice())
			.param(item.getQuantity())
			.param(now)
			.param(now)
			.query(UUID.class)
			.single();
		item.setId(id);
		item.setCreatedAt(now);
		item.setUpdatedAt(now);
	}

	private void updateCartItem(CartItem item) {
		String sql = """
				UPDATE cart_items
				SET product_name = ?, price = ?, quantity = ?, updated_at = ?
				WHERE id = ?
				""";
		OffsetDateTime now = OffsetDateTime.now(this.clock);
		jdbcClient.sql(sql)
			.param(item.getProductName())
			.param(item.getPrice())
			.param(item.getQuantity())
			.param(now)
			.param(item.getId())
			.update();
		item.setUpdatedAt(now);
	}

	private void deleteCartItems(List<UUID> itemIds) {
		if (itemIds.isEmpty()) {
			return;
		}
		String sql = "DELETE FROM cart_items WHERE id IN (:itemIds)";
		jdbcClient.sql(sql).param("itemIds", itemIds).update();
	}

	private boolean cartExists(UUID cartId) {
		String sql = "SELECT COUNT(*) FROM carts WHERE id = ?";
		Integer count = jdbcClient.sql(sql).param(cartId).query(Integer.class).single();
		return count > 0;
	}

}
