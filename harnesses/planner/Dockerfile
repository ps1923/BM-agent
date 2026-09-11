FROM python:3.12-slim
WORKDIR /app
COPY harnesses/planner/requirements.txt requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY harnesses harnesses
RUN useradd --system --uid 10002 harness
USER harness
EXPOSE 8102
CMD ["python", "-m", "harnesses.planner.service"]
