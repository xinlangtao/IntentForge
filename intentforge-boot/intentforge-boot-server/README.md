# intentforge-boot-server

Minimal server-side bootstrap entrypoint for the event-driven coding agent MVP.

Current implementation:
- JDK `HttpServer`
- HTTP JSON endpoints + SSE event stream
- request handling prefers virtual threads
- thin `HttpExchange` adapters that delegate business logic into `intentforge-api`
- create-run requests may auto-create one session when `sessionId` is omitted
- terminal main: `cn.intentforge.boot.server.AiAssetServerMain`

Minimal startup:

```bash
cd /Users/clouds3n/Coding/open-source/ai/intent-forge

./mvnw -q -Drevision=nightly-SNAPSHOT \
  -pl intentforge-boot/intentforge-boot-server \
  -am \
  -DskipTests \
  package dependency:build-classpath \
  -Dmdep.outputFile=/tmp/intentforge-boot-server.cp

CLASSPATH="intentforge-boot/intentforge-boot-server/target/classes:$(cat /tmp/intentforge-boot-server.cp)"

java -Dintentforge.server.port=18080 \
  -cp "$CLASSPATH" \
  cn.intentforge.boot.server.AiAssetServerMain
```

Startup output includes:
- base URL like `http://127.0.0.1:18080`
- create run endpoint path
- request handling prefers virtual threads
- no runtime seed data is preloaded by the main entrypoint

For end-to-end demo data, use the integration fixtures under `src/test/java`.
