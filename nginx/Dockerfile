FROM nginx:1.27-alpine

# Remove config padrão
RUN rm /etc/nginx/conf.d/default.conf

# Copia configuração do PGEO
COPY nginx.conf /etc/nginx/nginx.conf

# O diretório de certs é montado via volume no docker-compose.yml
# (não incluir chave privada na imagem)
VOLUME ["/etc/nginx/certs"]

EXPOSE 80 443
