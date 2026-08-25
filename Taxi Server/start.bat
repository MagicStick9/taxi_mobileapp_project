@echo off
:: Кодировка UTF-8, чтобы корректно отображался русский язык и эмодзи
chcp 65001 > nul
title Панель Управления Сервером

cls
echo ===================================================
echo   🚀 ЗАПУСК ИНФРАСТРУКТУРЫ BACKEND (NodeJS + Mongo)
echo ===================================================
echo.

:: ===================================================
:: Проверка необходимых файлов
:: ===================================================

echo [0/4] 🔍 Проверка файлов проекта...

if not exist "Dockerfile" (
    echo ❌ ОШИБКА: Dockerfile не найден!
    pause
    exit /b 1
)

if not exist "docker-compose.yml" (
    echo ❌ ОШИБКА: docker-compose.yml не найден!
    pause
    exit /b 1
)

if not exist "server.js" (
    echo ❌ ОШИБКА: server.js не найден!
    pause
    exit /b 1
)

if not exist "admin.html" (
    echo ❌ ОШИБКА: admin.html не найден!
    pause
    exit /b 1
)

if not exist "mysecret.env" (
    echo ❌ ОШИБКА: mysecret.env не найден!
    pause
    exit /b 1
)

if not exist "package.json" (
    echo ❌ ОШИБКА: package.json не найден!
    pause
    exit /b 1
)

if not exist "package-lock.json" (
    echo ❌ ОШИБКА: package-lock.json не найден!
    pause
    exit /b 1
)

echo ✅ Все необходимые файлы найдены.
echo.


:: ===================================================
:: Остановка старого приложения
:: ===================================================

echo [1/4] 🧹 Удаление старого APP контейнера и image...

docker compose down --rmi local

if errorlevel 1 (
    echo.
    echo ❌ ОШИБКА при остановке старой инфраструктуры!
    pause
    exit /b 1
)

echo ✅ Старый APP image удалён.
echo 💾 Данные MongoDB НЕ удалялись.
echo.


:: ===================================================
:: Полностью новая сборка
:: ===================================================

echo [2/4] 🐳 Создание НОВОГО Docker image...
echo.
echo     --no-cache = старые build layers НЕ используются
echo     --pull     = проверяется свежий base image
echo.

docker compose build --no-cache --pull app

if errorlevel 1 (
    echo.
    echo ===================================================
    echo ❌ ОШИБКА СБОРКИ Docker!
    echo ===================================================
    echo.
    echo Проверь Dockerfile, server.js и admin.html.
    echo.
    pause
    exit /b 1
)

echo.
echo ✅ Новый APP image успешно собран.
echo.


:: ===================================================
:: Запуск контейнеров
:: ===================================================

echo [3/4] 🚀 Запуск Docker контейнеров...

docker compose up -d

if errorlevel 1 (
    echo.
    echo ===================================================
    echo ❌ ОШИБКА ЗАПУСКА Docker!
    echo ===================================================
    echo.
    pause
    exit /b 1
)

echo.
echo ✅ Контейнеры успешно запущены.
echo.


:: ===================================================
:: Открытие Admin Panel
:: ===================================================

echo [4/4] 🌐 Открытие Админ-панели в браузере...

:: Небольшая пауза, чтобы Node.js успел подключиться к MongoDB
timeout /t 2 /nobreak > nul

start http://localhost:3000/admin

echo.
echo ===================================================
echo   ✨ Все системы успешно запущены!
echo ===================================================
echo.
echo   🌐 Админка:
echo      http://localhost:3000/admin
echo.
echo   🐳 APP:
echo      my-node-container
echo.
echo   🍃 MongoDB:
echo      mongodb-custom
echo.
echo   🔐 JWT:
echo      берётся из mysecret.env
echo.
echo   💾 MongoDB volume сохранён.
echo ===================================================
echo 💻 Нажмите любую клавишу, чтобы свернуть это окно...
echo ===================================================

pause > nul


