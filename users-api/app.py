import os
import sqlite3
from flask import Flask, request, jsonify

app = Flask(__name__)

DB_PATH = os.getenv("DB_PATH", "/data/users.db")

def init_db():
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    conn = sqlite3.connect(DB_PATH, timeout=30.0)
    conn.execute("PRAGMA journal_mode=WAL;")
    cursor = conn.cursor()
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            email TEXT NOT NULL UNIQUE,
            password TEXT NOT NULL
        )
    """)
    conn.commit()
    conn.close()

def get_db():
    conn = sqlite3.connect(DB_PATH, timeout=30.0)
    conn.row_factory = sqlite3.Row
    return conn

@app.route("/", methods=["GET"])
def root():
    return jsonify({
        "service": "Users API (Python)",
        "status": "running",
        "endpoints": {
            "GET /": "Informacion del servicio",
            "GET /users": "Consultar lista de usuarios",
            "POST /users": "Registrar nuevo usuario",
            "GET /users/<id>": "Consultar usuario por ID",
            "PUT /users/<id>": "Actualizar usuario por ID",
            "DELETE /users/<id>": "Eliminar usuario por ID"
        }
    }), 200

@app.route("/users", methods=["POST"])
def create_user():
    data = request.get_json(force=True)
    name = data.get("name")
    email = data.get("email")
    password = data.get("password")

    if not name or not email or not password:
        return jsonify({"error": "Missing required fields (name, email, password)"}), 400

    conn = get_db()
    try:
        cursor = conn.cursor()
        cursor.execute("INSERT INTO users (name, email, password) VALUES (?, ?, ?)", (name, email, password))
        conn.commit()
        user_id = cursor.lastrowid
        return jsonify({"id": user_id, "name": name, "email": email}), 201
    except sqlite3.IntegrityError:
        return jsonify({"error": "Email already exists"}), 409
    finally:
        conn.close()

@app.route("/users", methods=["GET"])
def get_users():
    conn = get_db()
    try:
        cursor = conn.cursor()
        cursor.execute("SELECT id, name, email FROM users ORDER BY id DESC")
        rows = cursor.fetchall()
        users = [{"id": row["id"], "name": row["name"], "email": row["email"]} for row in rows]
        return jsonify(users), 200
    finally:
        conn.close()

@app.route("/users/<int:user_id>", methods=["GET"])
def get_user(user_id):
    conn = get_db()
    try:
        cursor = conn.cursor()
        cursor.execute("SELECT id, name, email FROM users WHERE id = ?", (user_id,))
        row = cursor.fetchone()
        if not row:
            return jsonify({"error": "User not found"}), 404
        return jsonify({"id": row["id"], "name": row["name"], "email": row["email"]}), 200
    finally:
        conn.close()

@app.route("/users/<int:user_id>", methods=["PUT"])
def update_user(user_id):
    data = request.get_json(force=True)
    name = data.get("name")
    email = data.get("email")
    password = data.get("password")

    conn = get_db()
    try:
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM users WHERE id = ?", (user_id,))
        user = cursor.fetchone()
        if not user:
            return jsonify({"error": "User not found"}), 404

        new_name = name if name is not None else user["name"]
        new_email = email if email is not None else user["email"]
        new_password = password if password is not None else user["password"]

        cursor.execute("UPDATE users SET name = ?, email = ?, password = ? WHERE id = ?",
                       (new_name, new_email, new_password, user_id))
        conn.commit()
        return jsonify({"id": user_id, "name": new_name, "email": new_email}), 200
    except sqlite3.IntegrityError:
        return jsonify({"error": "Email already exists"}), 409
    finally:
        conn.close()

@app.route("/users/<int:user_id>", methods=["DELETE"])
def delete_user(user_id):
    conn = get_db()
    try:
        cursor = conn.cursor()
        cursor.execute("SELECT id FROM users WHERE id = ?", (user_id,))
        if not cursor.fetchone():
            return jsonify({"error": "User not found"}), 404

        cursor.execute("DELETE FROM users WHERE id = ?", (user_id,))
        conn.commit()
        return jsonify({"message": f"User {user_id} deleted successfully"}), 200
    finally:
        conn.close()

if __name__ == "__main__":
    init_db()
    app.run(host="0.0.0.0", port=8000)
