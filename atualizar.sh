#!/bin/bash

echo "--- INICIANDO ATUALIZAÇÃO DO PROJETO ---"

# Adiciona todas as alterações feitas no código
git add .

# Pergunta qual foi a alteração para colocar na mensagem do commit
echo "nada apenas um testa:"
read mensagem

# Faz o commit com a mensagem digitada
git commit -m "$mensagem"

# Envia para o GitHub na branch main
git push origin main

echo "--- PROJETO ATUALIZADO COM SUCESSO NO GITHUB! ---"
