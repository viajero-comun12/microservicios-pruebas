const express = require('express');
const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const fs = require('fs');

const app = express();
app.use(express.json());

const DB_PATH = process.env.DB_PATH || '/data/products.db';

// Ensure directory exists
const dbDir = path.dirname(DB_PATH);
if (!fs.existsSync(dbDir)) {
  fs.mkdirSync(dbDir, { recursive: true });
}

// Open SQLite database
const db = new sqlite3.Database(DB_PATH, (err) => {
  if (err) {
    console.error('Failed to open database:', err.message);
    process.exit(1);
  }
  console.log(`Connected to products SQLite database at ${DB_PATH}`);
  initDB();
});

function initDB() {
  db.run(`
    CREATE TABLE IF NOT EXISTS products (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      price REAL NOT NULL,
      description TEXT,
      stock INTEGER NOT NULL DEFAULT 0
    )
  `, (err) => {
    if (err) {
      console.error('Error creating products table:', err.message);
    } else {
      console.log('Products table verified/created.');
    }
  });
}

// 0. Ruta raiz informativa (GET /)
app.get('/', (req, res) => {
  res.json({
    service: 'Products API (JavaScript / Node.js)',
    status: 'running',
    endpoints: {
      'GET /': 'Informacion del servicio',
      'GET /products': 'Consultar lista de productos',
      'POST /products': 'Registrar nuevo producto',
      'GET /products/:id': 'Consultar producto por ID',
      'PUT /products/:id': 'Actualizar producto por ID',
      'DELETE /products/:id': 'Eliminar producto por ID'
    }
  });
});

// 1. Create a product (POST /products)
app.post('/products', (req, res) => {
  const { name, price, description, stock } = req.body;

  if (!name || price === undefined) {
    return res.status(400).json({ error: 'name and price are required' });
  }

  const sql = `INSERT INTO products (name, price, description, stock) VALUES (?, ?, ?, ?)`;
  const params = [name, price, description || '', stock !== undefined ? stock : 0];

  db.run(sql, params, function (err) {
    if (err) {
      return res.status(500).json({ error: err.message });
    }
    const newId = this.lastID;
    db.get(`SELECT * FROM products WHERE id = ?`, [newId], (err, row) => {
      if (err) {
        return res.status(500).json({ error: err.message });
      }
      res.status(201).json(row);
    });
  });
});

// 2. Get all products (GET /products)
app.get('/products', (req, res) => {
  db.all(`SELECT * FROM products ORDER BY id DESC`, [], (err, rows) => {
    if (err) {
      return res.status(500).json({ error: err.message });
    }
    res.status(200).json(rows);
  });
});

// 3. Get product by ID (GET /products/:id)
app.get('/products/:id', (req, res) => {
  const id = req.params.id;
  db.get(`SELECT * FROM products WHERE id = ?`, [id], (err, row) => {
    if (err) {
      return res.status(500).json({ error: err.message });
    }
    if (!row) {
      return res.status(404).json({ error: 'Product not found' });
    }
    res.status(200).json(row);
  });
});

// 4. Update a product (PUT /products/:id)
app.put('/products/:id', (req, res) => {
  const id = req.params.id;
  const { name, price, description, stock } = req.body;

  db.get(`SELECT * FROM products WHERE id = ?`, [id], (err, row) => {
    if (err) {
      return res.status(500).json({ error: err.message });
    }
    if (!row) {
      return res.status(404).json({ error: 'Product not found' });
    }

    const updatedName = name !== undefined ? name : row.name;
    const updatedPrice = price !== undefined ? price : row.price;
    const updatedDescription = description !== undefined ? description : row.description;
    const updatedStock = stock !== undefined ? stock : row.stock;

    const sql = `UPDATE products SET name = ?, price = ?, description = ?, stock = ? WHERE id = ?`;
    db.run(sql, [updatedName, updatedPrice, updatedDescription, updatedStock, id], function (err) {
      if (err) {
        return res.status(500).json({ error: err.message });
      }
      res.status(200).json({
        id: Number(id),
        name: updatedName,
        price: updatedPrice,
        description: updatedDescription,
        stock: updatedStock
      });
    });
  });
});

// 5. Delete a product (DELETE /products/:id)
app.delete('/products/:id', (req, res) => {
  const id = req.params.id;
  db.run(`DELETE FROM products WHERE id = ?`, [id], function (err) {
    if (err) {
      return res.status(500).json({ error: err.message });
    }
    if (this.changes === 0) {
      return res.status(404).json({ error: 'Product not found' });
    }
    res.status(200).json({ message: `Product ${id} deleted successfully` });
  });
});

const PORT = 8003;
app.listen(PORT, '0.0.0.0', () => {
  console.log(`Products API listening on port ${PORT}...`);
});
