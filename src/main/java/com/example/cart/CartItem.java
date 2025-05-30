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
import java.util.Objects;
import java.util.UUID;

public class CartItem {

	private UUID id;

	private UUID cartId;

	private String productId;

	private String productName;

	private BigDecimal price;

	private Integer quantity;

	private OffsetDateTime createdAt;

	private OffsetDateTime updatedAt;

	public CartItem() {
	}

	public CartItem(UUID cartId, String productId, String productName, BigDecimal price, Integer quantity) {
		this.cartId = cartId;
		this.productId = productId;
		this.productName = productName;
		this.price = price;
		this.quantity = quantity;
	}

	public CartItem(UUID id, UUID cartId, String productId, String productName, BigDecimal price, Integer quantity,
			OffsetDateTime createdAt, OffsetDateTime updatedAt) {
		this.id = id;
		this.cartId = cartId;
		this.productId = productId;
		this.productName = productName;
		this.price = price;
		this.quantity = quantity;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getCartId() {
		return cartId;
	}

	public void setCartId(UUID cartId) {
		this.cartId = cartId;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public BigDecimal getTotalPrice() {
		return price.multiply(new BigDecimal(quantity));
	}

	@Override
	public String toString() {
		return "CartItem{" + "id=" + id + ", cartId=" + cartId + ", productId='" + productId + '\'' + ", productName='"
				+ productName + '\'' + ", price=" + price + ", quantity=" + quantity + ", createdAt=" + createdAt
				+ ", updatedAt=" + updatedAt + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof CartItem cartItem))
			return false;
		return Objects.equals(id, cartItem.id) && Objects.equals(cartId, cartItem.cartId)
				&& Objects.equals(productId, cartItem.productId) && Objects.equals(productName, cartItem.productName)
				&& Objects.equals(price, cartItem.price) && Objects.equals(quantity, cartItem.quantity);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, cartId, productId, productName, price, quantity);
	}

}
