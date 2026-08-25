const fs = require('fs');
if (fs.existsSync('./mysecret.env')) {
    require('dotenv').config({ path: './mysecret.env' });
}

const express = require('express');
const { MongoClient, ObjectId } = require('mongodb');
const path = require('path');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');

const app = express();

const url = process.env.MONGO_URL || "mongodb://localhost:27017";
const dbName = "myDb";

const PORT = 3000;
const JWT_SECRET = process.env.JWT_SECRET;

if (!JWT_SECRET) {
    throw new Error("JWT_SECRET is not configured");
}

let collection;

app.use(express.json({ limit: "10kb" }));


// =========================
// Database База данных
// =========================

async function connectDb() {
    try {
        const client = await MongoClient.connect(url);
        const db = client.db(dbName);

        collection = db.collection("myTable");

        // Unique check Email должен быть уникальным..
        await collection.createIndex(
            { email: 1 },
            { unique: true }
        );

        console.log("Connected to MongoDB");

    } catch (err) {
        console.error("DB connection error:", err);
        process.exit(1);
    }
}

// =========================
// Validation Валидация
// =========================

function validateName(name) {
    if (typeof name !== "string") {
        return false;
    }

    const value = name.trim();

    if (value.length < 2 || value.length > 50) {
        return false;
    }

    // Буквы Unicode, пробел и дефис.
    return /^[\p{L}]+(?:[ -][\p{L}]+)*$/u.test(value);
}


function normalizeEmail(email) {
    if (typeof email !== "string") {
        return "";
    }

    return email.trim().toLowerCase();
}


function validateEmail(email) {
    if (typeof email !== "string") {
        return false;
    }

    if (email.length > 254) {
        return false;
    }

    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}


function validatePassword(password) {
    if (typeof password !== "string") {
        return false;
    }

    // bcrypt имеет ограничение 72 bytes - байта.
    const byteLength = Buffer.byteLength(password, "utf8");

    if (byteLength < 8 || byteLength > 72) {
        return false;
    }

    // At least letter and number ONE - Хотя бы буква и цифра ОДНА.
    if (!/[A-Za-zА-Яа-яЁё]/u.test(password)) {
        return false;
    }

    if (!/\d/.test(password)) {
        return false;
    }

    return true;
}


// =========================
// JWT - JSON Web Token / JSON Веб Токен
// =========================

function createToken(user) {
    return jwt.sign(
        {
            sub: user._id.toString(),
            name: user.name,
            email: user.email
        },
        JWT_SECRET,
        {
            algorithm: "HS256",
            expiresIn: "2h",
            issuer: "proekt-api"
        }
    );
}


function authenticateToken(req, res, next) {
	
	// JWT endpoint нельзя отдавать из HTTP-кэша.
    res.set("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
    res.set("Pragma", "no-cache");
    res.set("Expires", "0");
	
    const authorization = req.headers.authorization;

    if (!authorization || !authorization.startsWith("Bearer ")) {
        return res.status(401).send({
            error: "UNAUTHORIZED",
            message: "Authentication required"
        });
    }

    const token = authorization.substring(7);

    try {
        const decoded = jwt.verify(
            token,
            JWT_SECRET,
            {
                algorithms: ["HS256"],
                issuer: "proekt-api"
            }
        );

        req.user = decoded;

        next();

    } catch (err) {
        return res.status(401).send({
            error: "INVALID_TOKEN",
            message: "Invalid or expired token"
        });
    }
}


// =========================
// Signup - Авторизация
// =========================

app.post("/signup", async (req, res) => {
    try {
        const name =
            typeof req.body.name === "string"
                ? req.body.name.trim()
                : "";

        const email = normalizeEmail(req.body.email);
        const password = req.body.password;

        // -------------------------
        // Validation Errors - Ошибки Валидации
        // -------------------------

        if (!validateName(name)) {
            return res.status(400).send({
                error: "INVALID_NAME",
                message:
                    "Имя должно содержать от 2 до 50 символов и состоять из букв, пробелов и дефиса."
            });
        }

        if (!validateEmail(email)) {
            return res.status(400).send({
                error: "INVALID_EMAIL",
                message:
                    "Введите корректный адрес электронной почты."
            });
        }

        if (!validatePassword(password)) {
            return res.status(400).send({
                error: "INVALID_PASSWORD",
                message:
                    "Пароль должен содержать от 8 до 72 байт, хотя бы одну букву и одну цифру."
            });
        }

        // -------------------------
        // Existing user - Существующий пользователь
        // -------------------------

        const existingUser = await collection.findOne({
            email: email
        });

        if (existingUser) {
            return res.status(409).send({
                error: "EMAIL_EXISTS",
                message: "Пользователь с таким email уже существует."
            });
        }

        // -------------------------
        // Password hash - Хэш паролей
        // -------------------------

        const passwordHash = await bcrypt.hash(
            password,
            12
        );

        // -------------------------
        // Create user - Создать пользователя
        // -------------------------

        const newUser = {
            name: name,
            email: email,
            passwordHash: passwordHash,
            createdAt: new Date()
        };

        const result = await collection.insertOne(newUser);

        return res.status(201).send({
            message: "User created successfully",
            user: {
                id: result.insertedId.toString(),
                name: newUser.name,
                email: newUser.email
            }
        });

    } catch (err) {

        // Защита от race condition при уникальном email.
        if (err.code === 11000) {
            return res.status(409).send({
                error: "EMAIL_EXISTS",
                message: "Пользователь с таким email уже существует."
            });
        }

        console.error("Signup error:", err);

        return res.status(500).send({
            error: "SERVER_ERROR",
            message: "Ошибка сервера при регистрации."
        });
    }
});


// =========================
// Login - Авторизация
// =========================

app.post("/login", async (req, res) => {
    try {
        const email = normalizeEmail(req.body.email);
        const password = req.body.password;

        if (!validateEmail(email) ||
            typeof password !== "string" ||
            password.length === 0) {

            return res.status(400).send({
                error: "INVALID_INPUT",
                message: "Проверьте email и пароль."
            });
        }

        const user = await collection.findOne({
            email: email
        });

        if (!user) {
            return res.status(401).send({
                error: "INVALID_CREDENTIALS",
                message: "Неверный email или пароль."
            });
        }

        let passwordValid = false;

        // ==========================================
        // New Format - Новый формат: passwordHash
        // ==========================================

        if (user.passwordHash) {

            passwordValid = await bcrypt.compare(
                password,
                user.passwordHash
            );

        }

        // ==========================================
        // LEGACY SUPPORT Поддержка Легаси (Cursed / Проклято)
        //
        // UPD - Апдейт
        //
        // Old users could have - Старые пользователи могли иметь:
        // name: "#@-123"
        // email: "123"
        // password: "123"
        //
        // Trying not to break them. Не ломаем их.
        // Success entering transfer - После успешного входа переводим
        // Account to - аккаунт на passwordHash.
        // ==========================================

        else if (typeof user.password === "string") {

            passwordValid =
                password === user.password;

            if (passwordValid) {

                const passwordHash =
                    await bcrypt.hash(password, 12);

                await collection.updateOne(
                    { _id: user._id },
                    {
                        $set: {
                            passwordHash: passwordHash,
                            passwordMigratedAt: new Date()
                        },
                        $unset: {
                            password: ""
                        }
                    }
                );
            }
        }

        if (!passwordValid) {
            return res.status(401).send({
                error: "INVALID_CREDENTIALS",
                message: "Неверный email или пароль."
            });
        }

        // ==========================================
        // Tokens JWT / Токены JWT
        // ==========================================

        const token = createToken(user);

        return res.status(200).send({
            token: token,
            name: user.name,
            email: user.email,
        });

    } catch (err) {

        console.error("Login error:", err);

        return res.status(500).send({
            error: "SERVER_ERROR",
            message: "Ошибка сервера при входе."
        });
    }
});


// =========================
// Admin Panel HTML / Админ Панель ХТМЛ 
// =========================

app.get("/admin", (req, res) => {
    res.sendFile(
        path.join(__dirname, "admin.html")
    );
});


// =========================
// Admin: View All Users - Показ Всех Пользователей
// =========================

app.get("/admin/users", async (req, res) => {
    try {

        res.set(
            "Cache-Control",
            "no-store, no-cache, must-revalidate, proxy-revalidate"
        );

        res.set("Pragma", "no-cache");
        res.set("Expires", "0");

        const users = await collection
            .find({})
            .project({
                password: 0,
                passwordHash: 0
            })
            .toArray();

        return res.status(200).send(users);

    } catch (err) {

        console.error("Error fetching users:", err);

        return res.status(500).send({
            error: "SERVER_ERROR",
            message: "Ошибка получения пользователей."
        });
    }
});



// =========================
// Admin: Delete User - Удаление Пользователя
// =========================

app.delete("/admin/users/:id", async (req, res) => {
    try {

        if (!ObjectId.isValid(req.params.id)) {
            return res.status(400).send({
                error: "INVALID_ID",
                message: "Некорректный ID пользователя."
            });
        }

        const result =
            await collection.deleteOne({
                _id: new ObjectId(req.params.id)
            });

        if (result.deletedCount === 1) {
            return res.status(200).send({
                message: "User deleted successfully"
            });
        }

        return res.status(404).send({
            error: "USER_NOT_FOUND",
            message: "User not found"
        });

    } catch (err) {

        console.error("Delete error:", err);

        return res.status(500).send({
            error: "SERVER_ERROR",
            message: "Ошибка удаления пользователя."
        });
    }
});


// =========================
// Admin: Update User List - Обновление Списка Пользователей
// =========================

app.put("/admin/users/:id", async (req, res) => {
    try {

        if (!ObjectId.isValid(req.params.id)) {
            return res.status(400).send({
                error: "INVALID_ID",
                message: "Некорректный ID пользователя."
            });
        }

        const name =
            typeof req.body.name === "string"
                ? req.body.name.trim()
                : "";

        const email =
            normalizeEmail(req.body.email);

        if (!validateName(name)) {
            return res.status(400).send({
                error: "INVALID_NAME",
                message: "Некорректное имя."
            });
        }

        if (!validateEmail(email)) {
            return res.status(400).send({
                error: "INVALID_EMAIL",
                message: "Некорректный email."
            });
        }

        const updatedUser = {
            name: name,
            email: email
        };

        const result =
            await collection.updateOne(
                {
                    _id: new ObjectId(req.params.id)
                },
                {
                    $set: updatedUser
                }
            );

        if (result.matchedCount === 0) {
            return res.status(404).send({
                error: "USER_NOT_FOUND",
                message: "User not found"
            });
        }

        return res.status(200).send({
            message: "User updated"
        });

    } catch (err) {

        if (err.code === 11000) {
            return res.status(409).send({
                error: "EMAIL_EXISTS",
                message: "Пользователь с таким email уже существует."
            });
        }

        console.error("Update error:", err);

        return res.status(500).send({
            error: "SERVER_ERROR",
            message: "Ошибка обновления пользователя."
        });
    }
});


// =========================
// Example protected route
// =========================
//
// JWT здесь уже реально используется.
// Остальные существующие маршруты пока
// намеренно НЕ переводим на него,
// чтобы не ломать текущую логику.
//
// =========================

app.get("/profile", authenticateToken, async (req, res) => {
    try {

        const user = await collection.findOne(
            {
                _id: new ObjectId(req.user.sub)
            },
            {
                projection: {
                    password: 0,
                    passwordHash: 0
                }
            }
        );

        if (!user) {
            return res.status(404).send({
                error: "USER_NOT_FOUND",
                message: "User not found"
            });
        }

        return res.status(200).send({
            id: user._id.toString(),
            name: user.name,
            email: user.email
        });

    } catch (err) {

        console.error("Profile error:", err);

        return res.status(500).send({
            error: "SERVER_ERROR",
            message: "Ошибка получения профиля."
        });
    }
});


// =========================
// Start - Старт
// =========================

async function startServer() {
    await connectDb();

    app.listen(PORT, () => {
        console.log(
            `Server running on http://localhost:${PORT}`
        );
    });
}

startServer();
