Environment variables (set in your deployment or compose .env):

- CORS_ALLOWED_ORIGINS: Comma-separated list of allowed origins for CORS, e.g. "https://example.com,https://www.example.com".
- JWT_SECRET: Strong secret for JWT signing.
- SPRING_DATASOURCE_URL / USERNAME / PASSWORD: Database connection.
- SPRING_REDIS_HOST / PORT: Redis connection.
- SPRING_RABBITMQ_HOST / PORT: RabbitMQ connection.
- REACT_APP_API_URL: Frontend to backend API base URL.

Example (prod):

```
CORS_ALLOWED_ORIGINS=https://example.com,https://www.example.com
JWT_SECRET=use-a-strong-random-secret
REACT_APP_API_URL=https://api.example.com
```

