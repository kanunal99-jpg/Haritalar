from pathlib import Path

path = Path("app/src/main/java/com/haritalar/app/MainActivity.kt")
text = path.read_text()

old = '''                    val ranked = trafficRankingService.rankBlocking(candidates)\n                    RouteTrafficPresentation.fromRanked(ranked)\n                        .let { models -> candidates.map { it.routeId }.zip(models).toMap() }\n'''
new = '''                    val ranked = trafficRankingService.rankBlocking(candidates)\n                    ranked.zip(RouteTrafficPresentation.fromRanked(ranked))\n                        .associate { (candidate, model) -> candidate.routeId to model }\n'''

if old not in text:
    if 'ranked.zip(RouteTrafficPresentation.fromRanked(ranked))' in text:
        print("Traffic ranking presentation identity fix already present; no patch needed.")
        raise SystemExit(0)
    raise SystemExit("Expected traffic presentation mapping was not found; refusing to patch blindly.")

path.write_text(text.replace(old, new, 1))
print("Traffic ranking presentation identity fix applied.")
