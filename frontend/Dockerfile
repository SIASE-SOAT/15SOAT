# ---- Build Stage ----
FROM node:20-alpine AS builder

WORKDIR /app

# Copia package.json e package-lock.json
COPY package*.json ./

# Instala dependências
RUN npm ci

# Copia código-fonte
COPY . .

# Build da aplicação Angular em modo produção
RUN npm run build

# ---- Runtime Stage com Nginx ----
FROM nginx:alpine

# Remove nginx config padrão
RUN rm -rf /etc/nginx/conf.d/*

# Copia configuração nginx customizada
COPY nginx.conf /etc/nginx/nginx.conf

# Copia o build da stage anterior para o local correto do nginx
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 4200

# Inicia nginx
CMD ["nginx", "-g", "daemon off;"]

