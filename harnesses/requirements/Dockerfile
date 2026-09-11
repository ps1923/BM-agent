FROM python:3.12-slim
WORKDIR /app
COPY harnesses/requirements/requirements.txt requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY harnesses harnesses
RUN useradd --system --uid 10001 harness
USER harness
EXPOSE 8101
CMD ["python", "-m", "harnesses.requirements.service"]
