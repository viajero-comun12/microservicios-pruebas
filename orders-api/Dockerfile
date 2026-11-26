# Build stage
FROM golang:1.22-alpine AS build

WORKDIR /app

COPY . .
RUN go mod tidy && CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -o orders-api .

# Run stage
FROM alpine:3.19

WORKDIR /app
COPY --from=build /app/orders-api .

EXPOSE 8002

CMD ["./orders-api"]
