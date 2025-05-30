# Cart API

This is a simple demo app (Cart API) for testing Amazon Aurora DSQL.
To access DSQL, please modify `spring.datasource.url` to match your environment. You also need to provide appropriate credentials for issuing DSQL tokens. 
For how to provide credentials, please refer to the [Spring Cloud AWS documentation](https://docs.awspring.io/spring-cloud-aws/docs/3.3.1/reference/html/index.html#credentials).


## How to use the Cart API

```bash
# 1. Create/Get cart
curl -s "http://localhost:8080/api/v1/carts?userId=user123" | jq .

# 2. Add item
curl -s -X POST "http://localhost:8080/api/v1/carts/items?userId=user123" \
  --json '{
    "productId": "product-001",
    "productName": "iPhone 15",
    "price": 999.99,
    "quantity": 1
  }' | jq .

# 3. Add another item
curl -s -X POST "http://localhost:8080/api/v1/carts/items?userId=user123" \
  --json '{
    "productId": "product-002",
    "productName": "MacBook Pro",
    "price": 2499.99,
    "quantity": 1
  }' | jq .

# 4. Check cart contents
curl -s "http://localhost:8080/api/v1/carts?userId=user123" | jq .

# 5. Update quantity (change quantity of first item to 3)
ITEM_ID=$(curl -s "http://localhost:8080/api/v1/carts?userId=user123" | jq -r ".items[0].id")
curl -s -X PATCH "http://localhost:8080/api/v1/carts/items/${ITEM_ID}?userId=user123" \
  --json '{"quantity": 3}' | jq .

# 6. Delete item
curl -s -X DELETE "http://localhost:8080/api/v1/carts/items/${ITEM_ID}?userId=user123" | jq .
```

```bash
# 7. Delete cart

curl -s -X DELETE "http://localhost:8080/api/v1/carts?userId=user123" | jq .
```

## How to run with PostgreSQL using Testcontainers instead of DSQL

```
./mvnw spring-boot:test-run
```

## How to trigger optimistic locking errors

Please install [`vegeta`](https://github.com/tsenart/vegeta).

```bash
# Create a cart if not exists
curl -s "http://localhost:8080/api/v1/carts?userId=user123" | jq .
# Clear the cart
curl -s -X DELETE "http://localhost:8080/api/v1/carts/items?userId=user123" | jq .
# Add an item to the cart
curl -s -X POST "http://localhost:8080/api/v1/carts/items?userId=user123" \
  --json '{
    "productId": "product-001",
    "productName": "iPhone 15",
    "price": 999.99,
    "quantity": 1
  }' | jq .
ITEM_ID=$(curl -s "http://localhost:8080/api/v1/carts?userId=user123" | jq -r ".items[0].id")

cat <<EOF > body.json
{
  "quantity": 3
}
EOF

# Run the attack
echo "PATCH http://localhost:8080/api/v1/carts/items/${ITEM_ID}?userId=user123" | vegeta attack -duration=10s -rate=30 -body=body.json -header='Content-Type: application/json' | vegeta report
```

You will see the following output:

```
2025-05-30T14:50:39.882+09:00  INFO 94356 --- [demo-dsql] [omcat-handler-9] [                                                 ] com.example.retry.RetryLoggingListener   : onError: [RetryContext: count=1, lastException=org.springframework.dao.OptimisticLockingFailureException: ERROR: change conflicts with another transaction, please retry: (OC000), exhausted=false]
```