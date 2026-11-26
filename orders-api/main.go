package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strconv"

	_ "modernc.org/sqlite"
)

type Order struct {
	ID              int64   `json:"id"`
	CustomerName    string  `json:"customer_name"`
	ItemDescription string  `json:"item_description"`
	Amount          float64 `json:"amount"`
	Status          string  `json:"status"`
	CreatedAt       string  `json:"created_at"`
}

var db *sql.DB

func initDB() {
	dbPath := os.Getenv("DB_PATH")
	if dbPath == "" {
		dbPath = "/data/orders.db"
	}

	dir := filepath.Dir(dbPath)
	if err := os.MkdirAll(dir, 0755); err != nil {
		log.Fatalf("Failed to create db directory: %v", err)
	}

	var err error
	db, err = sql.Open("sqlite", dbPath)
	if err != nil {
		log.Fatalf("Failed to open sqlite database: %v", err)
	}

	createTableSQL := `
	CREATE TABLE IF NOT EXISTS orders (
		id INTEGER PRIMARY KEY AUTOINCREMENT,
		customer_name TEXT NOT NULL,
		item_description TEXT NOT NULL,
		amount REAL NOT NULL,
		status TEXT NOT NULL,
		created_at DATETIME DEFAULT CURRENT_TIMESTAMP
	);`

	if _, err := db.Exec(createTableSQL); err != nil {
		log.Fatalf("Failed to initialize orders table: %v", err)
	}
	log.Printf("Orders database initialized at %s", dbPath)
}

func sendJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}

func createOrder(w http.ResponseWriter, r *http.Request) {
	var req struct {
		CustomerName    string  `json:"customer_name"`
		ItemDescription string  `json:"item_description"`
		Amount          float64 `json:"amount"`
		Status          string  `json:"status"`
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, map[string]string{"error": "Invalid JSON body"})
		return
	}

	if req.CustomerName == "" || req.ItemDescription == "" {
		sendJSON(w, http.StatusBadRequest, map[string]string{"error": "customer_name and item_description are required"})
		return
	}

	if req.Status == "" {
		req.Status = "PENDING"
	}

	res, err := db.Exec(
		"INSERT INTO orders (customer_name, item_description, amount, status) VALUES (?, ?, ?, ?)",
		req.CustomerName, req.ItemDescription, req.Amount, req.Status,
	)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	id, _ := res.LastInsertId()

	var order Order
	err = db.QueryRow("SELECT id, customer_name, item_description, amount, status, created_at FROM orders WHERE id = ?", id).
		Scan(&order.ID, &order.CustomerName, &order.ItemDescription, &order.Amount, &order.Status, &order.CreatedAt)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	sendJSON(w, http.StatusCreated, order)
}

func getOrders(w http.ResponseWriter, r *http.Request) {
	rows, err := db.Query("SELECT id, customer_name, item_description, amount, status, created_at FROM orders ORDER BY id DESC")
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	defer rows.Close()

	orders := []Order{}
	for rows.Next() {
		var o Order
		if err := rows.Scan(&o.ID, &o.CustomerName, &o.ItemDescription, &o.Amount, &o.Status, &o.CreatedAt); err != nil {
			sendJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
			return
		}
		orders = append(orders, o)
	}

	sendJSON(w, http.StatusOK, orders)
}

func getOrderByID(w http.ResponseWriter, r *http.Request) {
	idStr := r.PathValue("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		sendJSON(w, http.StatusBadRequest, map[string]string{"error": "Invalid order ID"})
		return
	}

	var o Order
	err = db.QueryRow("SELECT id, customer_name, item_description, amount, status, created_at FROM orders WHERE id = ?", id).
		Scan(&o.ID, &o.CustomerName, &o.ItemDescription, &o.Amount, &o.Status, &o.CreatedAt)
	if err == sql.ErrNoRows {
		sendJSON(w, http.StatusNotFound, map[string]string{"error": "Order not found"})
		return
	} else if err != nil {
		sendJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	sendJSON(w, http.StatusOK, o)
}

func updateOrder(w http.ResponseWriter, r *http.Request) {
	idStr := r.PathValue("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		sendJSON(w, http.StatusBadRequest, map[string]string{"error": "Invalid order ID"})
		return
	}

	// Check if order exists
	var o Order
	err = db.QueryRow("SELECT id, customer_name, item_description, amount, status, created_at FROM orders WHERE id = ?", id).
		Scan(&o.ID, &o.CustomerName, &o.ItemDescription, &o.Amount, &o.Status, &o.CreatedAt)
	if err == sql.ErrNoRows {
		sendJSON(w, http.StatusNotFound, map[string]string{"error": "Order not found"})
		return
	} else if err != nil {
		sendJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	var req struct {
		Status          *string  `json:"status"`
		CustomerName    *string  `json:"customer_name"`
		ItemDescription *string  `json:"item_description"`
		Amount          *float64 `json:"amount"`
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, map[string]string{"error": "Invalid JSON body"})
		return
	}

	if req.Status != nil {
		o.Status = *req.Status
	}
	if req.CustomerName != nil {
		o.CustomerName = *req.CustomerName
	}
	if req.ItemDescription != nil {
		o.ItemDescription = *req.ItemDescription
	}
	if req.Amount != nil {
		o.Amount = *req.Amount
	}

	_, err = db.Exec(
		"UPDATE orders SET customer_name = ?, item_description = ?, amount = ?, status = ? WHERE id = ?",
		o.CustomerName, o.ItemDescription, o.Amount, o.Status, id,
	)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	sendJSON(w, http.StatusOK, o)
}

func deleteOrder(w http.ResponseWriter, r *http.Request) {
	idStr := r.PathValue("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		sendJSON(w, http.StatusBadRequest, map[string]string{"error": "Invalid order ID"})
		return
	}

	res, err := db.Exec("DELETE FROM orders WHERE id = ?", id)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	rowsAffected, _ := res.RowsAffected()
	if rowsAffected == 0 {
		sendJSON(w, http.StatusNotFound, map[string]string{"error": "Order not found"})
		return
	}

	sendJSON(w, http.StatusOK, map[string]string{"message": fmt.Sprintf("Order %d deleted successfully", id)})
}

func main() {
	initDB()

	mux := http.NewServeMux()
	mux.HandleFunc("GET /{$}", func(w http.ResponseWriter, r *http.Request) {
		sendJSON(w, http.StatusOK, map[string]interface{}{
			"service": "Orders API (Go)",
			"status":  "running",
			"endpoints": map[string]string{
				"GET /":            "Informacion del servicio",
				"GET /orders":      "Consultar todos los pedidos",
				"POST /orders":     "Crear un nuevo pedido",
				"GET /orders/{id}": "Consultar un pedido por ID",
				"PUT /orders/{id}": "Actualizar pedido por ID",
				"DELETE /orders/{id}": "Eliminar pedido por ID",
			},
		})
	})
	mux.HandleFunc("POST /orders", createOrder)
	mux.HandleFunc("GET /orders", getOrders)
	mux.HandleFunc("GET /orders/{id}", getOrderByID)
	mux.HandleFunc("PUT /orders/{id}", updateOrder)
	mux.HandleFunc("DELETE /orders/{id}", deleteOrder)

	port := 8002
	log.Printf("Orders API listening on port %d...", port)
	if err := http.ListenAndServe(fmt.Sprintf(":%d", port), mux); err != nil {
		log.Fatalf("Server failed: %v", err)
	}
}
