# Используем Node для сборки и разработки
FROM node:22-alpine

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .

# Vite по умолчанию работает на 5173
EXPOSE 5173

# Запускаем в режиме разработки с хостом 0.0.0.0 для доступа извне контейнера
CMD ["npm", "run", "dev", "--", "--host"]
