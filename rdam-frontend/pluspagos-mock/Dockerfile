FROM node:20-alpine

WORKDIR /app

COPY server.js .

ENV PORT=8081
ENV PLUSPAGOS_SECRET=mock-secret-desarrollo
ENV BACKEND_URL=http://app:8080

EXPOSE 8081

HEALTHCHECK --interval=10s --timeout=5s --start-period=5s --retries=3 \
  CMD node -e "require('http').get('http://localhost:8081/health', (r) => {if(r.statusCode !== 404) process.exit(0); process.exit(1);})"

CMD ["node", "server.js"]
