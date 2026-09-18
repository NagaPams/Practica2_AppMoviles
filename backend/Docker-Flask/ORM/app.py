from flask import Flask, jsonify, request
from flask_sqlalchemy import SQLAlchemy
from flask_bcrypt import Bcrypt
from functools import wraps
import os
import datetime
import jwt

app = Flask(__name__)

# 1. Configuración de la Base de Datos (SQLite)
# El archivo se guardará en la carpeta del contenedor como 'site.db'
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///site.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

# Llave para firmar los tokens de sesión (JWT). Se toma de una variable de
# entorno para no dejarla escrita en el código; en docker-compose.yml se
# inyecta desde el archivo .env (ver .env.example).
app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY', 'dev-secret-key-cambiame')
TOKEN_EXP_HOURS = 2

db = SQLAlchemy(app)
bcrypt = Bcrypt(app)

# 2. Modelo de Usuario (La tabla en la BD)
class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(20), unique=True, nullable=False)
    password = db.Column(db.String(60), nullable=False) # Aquí guardaremos el hash

    def __repr__(self):
        return f"User('{self.username}')"

# --- INICIO CAMBIOS: Modelo para Libros (CRUD) ---
# Agregué la tabla Book a la base de datos para manejar mi recurso de "Compra de Libros"
class Book(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(100), nullable=False)
    author = db.Column(db.String(100), nullable=False)
    price = db.Column(db.Float, nullable=False)
    stock = db.Column(db.Integer, default=0)

    # Creé esta función auxiliar para convertir el objeto a JSON fácilmente
    def to_dict(self):
        return {
            "id": self.id,
            "title": self.title,
            "author": self.author,
            "price": self.price,
            "stock": self.stock
        }
# --- FIN CAMBIOS: Modelo para Libros ---

# --- INICIO CAMBIOS: Sesiones seguras (JWT) ---
def generar_token(user):
    payload = {
        "user_id": user.id,
        "username": user.username,
        "exp": datetime.datetime.utcnow() + datetime.timedelta(hours=TOKEN_EXP_HOURS)
    }
    return jwt.encode(payload, app.config['SECRET_KEY'], algorithm="HS256")


def token_required(f):
    # Decorador que exige un JWT válido en el header Authorization: Bearer <token>
    # Protege las rutas de CRUD para que solo usuarios autenticados las usen.
    @wraps(f)
    def decorated(*args, **kwargs):
        auth_header = request.headers.get('Authorization', '')

        if not auth_header.startswith('Bearer '):
            return jsonify({"message": "Falta el token de autenticación"}), 401

        token = auth_header.split(' ', 1)[1]

        try:
            jwt.decode(token, app.config['SECRET_KEY'], algorithms=["HS256"])
        except jwt.ExpiredSignatureError:
            return jsonify({"message": "El token ha expirado"}), 401
        except jwt.InvalidTokenError:
            return jsonify({"message": "Token inválido"}), 401

        return f(*args, **kwargs)
    return decorated
# --- FIN CAMBIOS: Sesiones seguras (JWT) ---

# 3. Rutas

@app.route('/')
def hello():
    return jsonify({"message": "API Funcionando"})

# Endpoint de REGISTRO
@app.route('/register', methods=['POST'])
def register():
    data = request.get_json()
    username = data.get('username')
    password = data.get('password')

    # Verificar si el usuario ya existe
    if User.query.filter_by(username=username).first():
        return jsonify({"message": "El usuario ya existe"}), 400

    # Encriptar contraseña
    hashed_password = bcrypt.generate_password_hash(password).decode('utf-8')
    
    # Crear y guardar nuevo usuario
    new_user = User(username=username, password=hashed_password)
    db.session.add(new_user)
    db.session.commit()

    return jsonify({"message": "Usuario creado exitosamente"}), 201

# Endpoint de LOGIN
@app.route('/login', methods=['POST'])
def login():
    data = request.get_json()
    username = data.get('username')
    password = data.get('password')

    user = User.query.filter_by(username=username).first()

    # Verificamos si el usuario existe y si la contraseña coincide con el hash
    if user and bcrypt.check_password_hash(user.password, password):
        token = generar_token(user)
        return jsonify({
            "status": "success",
            "message": "Login exitoso",
            "token": token,
            "user_id": user.id,
            "username": user.username
        }), 200
    else:
        return jsonify({"status": "error", "message": "Credenciales inválidas"}), 401

# --- INICIO CAMBIOS: Endpoints CRUD para Libros ---

# 1. CREAR un libro (POST)
# Implementé esta ruta para registrar nuevos libros en mi catálogo
@app.route('/books', methods=['POST'])
@token_required
def create_book():
    data = request.get_json()
    new_book = Book(
        title=data.get('title'),
        author=data.get('author'),
        price=data.get('price'),
        stock=data.get('stock', 0)
    )
    db.session.add(new_book)
    db.session.commit()
    return jsonify({"message": "Libro creado exitosamente", "book": new_book.to_dict()}), 201

# 2. LEER todos los libros (GET)
# Desarrollé este endpoint para consultar el catálogo completo
@app.route('/books', methods=['GET'])
@token_required
def get_books():
    books = Book.query.all()
    return jsonify([book.to_dict() for book in books]), 200

# 3. ACTUALIZAR un libro (PUT)
# Añadí esta ruta para actualizar un libro (por ejemplo, reducir el stock cuando realizo una compra)
@app.route('/books/<int:book_id>', methods=['PUT'])
@token_required
def update_book(book_id):
    book = Book.query.get(book_id)
    if not book:
        return jsonify({"message": "Libro no encontrado"}), 404
        
    data = request.get_json()
    book.title = data.get('title', book.title)
    book.author = data.get('author', book.author)
    book.price = data.get('price', book.price)
    book.stock = data.get('stock', book.stock)
    
    db.session.commit()
    return jsonify({"message": "Libro actualizado", "book": book.to_dict()}), 200

# 4. ELIMINAR un libro (DELETE)
# Creé este endpoint para poder borrar un libro de la base de datos
@app.route('/books/<int:book_id>', methods=['DELETE'])
@token_required
def delete_book(book_id):
    book = Book.query.get(book_id)
    if not book:
        return jsonify({"message": "Libro no encontrado"}), 404
        
    db.session.delete(book)
    db.session.commit()
    return jsonify({"message": "Libro eliminado exitosamente"}), 200

# --- FIN CAMBIOS: Endpoints CRUD ---

if __name__ == '__main__':
    # Esto crea las tablas automáticamente si no existen al iniciar
    with app.app_context():
        db.create_all()
    
    app.run(host='0.0.0.0', port=5000, debug=True)