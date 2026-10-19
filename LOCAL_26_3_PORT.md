# Private NeoForge 26.3 port checkpoint

## Source and scope

- Fork: athegreat90/ImmersiveAircraft; upstream Luke100000/ImmersiveAircraft.
- Branch: `26.3-local`; original `work` branch preserved. The user subsequently authorized publishing this branch to the athegreat90 fork.
- Base: upstream `26.2`, `ca215f55562127761b108766e3ddb791aed67756` (1.7.0+26.2).
- Source initially clean. Development and validation were local. Only branch publication to the user's fork is now authorized; no artifact releases or production changes. The inherited CI excludes `26.3-local` from push triggers to prevent automatic Maven publication.
- Target/version: Minecraft 26.3, NeoForge 26.3.0.94, Java 25, `1.7.0+26.3-local`.
- Original wrappers/plugins retained: Gradle 9.7.1, ModDevGradle 2.0.147. Checksum-verified Temurin 25.0.4.1+1 installed in `/workspace/.toolchains`.

## Completed migration

1. Configure only NeoForge; preserve common architecture and Fabric sources without porting them. Exact Minecraft range `[26.3]`, NeoForge `[26.3.0.94,26.4)`, actual pack versions resource97.1/data121.0. NeoForge icon metadata uses `iconFile`.
2. SDL: actual InputConstants scancodes/KEYBOARD, modifier/action/mouse constants and isKeyDown(int), retaining key registration IDs. Retained MidnightControls API is the only compile-only legacy GLFW consumer; it is not bundled or accepted as a 26.3 runtime.
3. Render: quaternion PoseStack.rotate(Quaternionfc), retaining matrix mulPose; new shouldRender partialTicks; DeferredRenderBuffer records/replays UV3. Blaze3D classes that remain there were not blindly renamed. No custom shaders exist to migrate.
4. Interpolation: override Entity.createInterpolationHandler with LinearInterpolationHandler.create(this,10), use native final getInterpolation and target() data. Same ten-step client interpolation; native server no-op. Pilot handover and real flight need further behavioral tests.
5. Fuel: removed FuelValues replaced with server-resolved immutable normal-furnace per-item COOKING_FUEL snapshot. Actual required loot context plus NeoForge QUERIED_STACK; unplaced furnace, no world mutation. Sync on login/datapack reload and clear on logout/stop. Existing custom config overrides preserved. AircraftDataMessage validates known item IDs, bounded count, positive durations, duplicates. Existing packet ID retained, wire protocol explicitly changed to `26.3-local-2`.
6. GameRenderer mixin timing captured from update(DeltaTracker), because actual 26.3 renderLevel has no arguments. Essential mixin requirements retained.
7. Registry/item/entity/component/persistent identifiers, licenses and attribution retained. `common/src/main/resources` byte-for-byte unchanged from upstream. No net VehicleScreenHandler change: its existing getter is sufficient for Machinery.

## Dependencies and limitations

- Compile integrations verified: Cloth26.3.159, JEI31.16.0.84, Controlify3.5.3+mc26.3, YACL3.9.7+26.3, signed Controllable0.26.2+26.3 and Framework0.13.27+26.3.
- `python3 gradle/prepare-local-dependencies.py` downloads the two signed NeoForge release assets with pinned SHA-256 verification to ignored local-dependencies. Required runtime dependencies: none beyond NeoForge; mXparser5.2.1 remains nested.
- Default development controller runtime none. MidnightControls has no verified26.3 release; existing1.12.1+26.2 API plus GLFW3.4.3 is compile-only. Metadata rejects client runtime versions through1.12.1; enabling that development runtime throws a clear unsupported error. Optional integrations have not been interactively tested.
- Context-dependent/per-stack modded fuel extensions remain UNTESTED. The snapshot preserves the former default per-item fuel registry semantics, not a promise about newly introduced dynamic extensions.

## Validation and current status

- Last full command: `GRADLE_USER_HOME=/workspace/.cache/gradle bash ./gradlew --no-daemon --max-workers=3 --console=plain :neoforge:build`, Java25, PASS (28s), `/workspace/.local-port/aircraft-build-final.log`.
- Three JUnit tests PASS, zero skipped/errors: simulated/committed bucket extraction without duplication; real fuel packet encode/decode/receive; malformed fuel entry rejection. XML: neoforge/build/test-results/test/TEST-immersive_aircraft.neoforge.Local263FuelTest.xml.
- Real-level server regression PASS: native furnace coal1600, Aircraft coal1600, stone0. JUnit's ephemeral server has no levels, so this assertion was moved to a real-level diagnostic mod instead of relaxing it. Initial fake-player riding fixture also corrected, independently of production behavior.
- Official-installer disposable dedicated26.3.0.94 server loaded both real runtime JARs, world created/reloaded, seven aircraft and four machines summoned. Nine configured fuel fields remained123 after reload; biplane and digger retained seven coal. Gyrodyne/Bee have no default Fuel0 and were verified via saved tags. Only smoke-test scope; original26.2 world compatibility UNTESTED (none supplied).
- Latest final artifact (icon metadata corrected) starts on the same dedicated server. Offscreen Mesa OpenGL4.5 client loads both real JARs, connects to the dedicated server and captures actual rendered vehicles. Biplane boarding/dismount was ordered from the server console; camera and fuel HUD rendered. This does not validate normal input/interaction, flight or multiple real players.
- Rendering, controls, full gameplay, upgrades, passengers and real multiple-user sessions are not comprehensively validated. Full manual checklist and installation/rollback are in `/workspace/local-26.3-jars/README.md`.
- Artifact: neoforge/build/libs/immersive_aircraft-neoforge-1.7.0+26.3-local.jar. Delivery copy/checksums: `/workspace/local-26.3-jars`; see RESULTADOS.md for final validation scope.

## References and next exact action

Official primer: https://github.com/neoforged/.github/blob/main/primers/26.3/index.md (docs site blocked; official repository copy read). Effective patched26.3 sources: Entity/LinearInterpolationHandler, PoseStack, GameRenderer, VertexConsumer, FurnaceBlockEntity/CookingFuel/ResolvableInt/LootContext, InputConstants. Sources reside in the NeoForm runtime transformSources cache. JUnit setup: https://github.com/neoforged/ModDevGradle#unit-testing-with-junit.

Final client log: `/workspace/.local-port/client-joined-console.log`. Next: complete the outstanding manual gameplay/two-player/26.2-world checklist on disposable copies. No pending production compiler errors. Final verification and process shutdown recorded in delivery RESULTADOS.md.

## Modified tracked files

```text
.gitignore
build.gradle
common/src/main/java/immersive_aircraft/client/KeyBindings.java
common/src/main/java/immersive_aircraft/client/MultiKeyMapping.java
common/src/main/java/immersive_aircraft/client/VehicleCameraTransform.java
common/src/main/java/immersive_aircraft/client/render/entity/renderer/AircraftEntityRenderer.java
common/src/main/java/immersive_aircraft/client/render/entity/renderer/GyrodyneEntityRenderer.java
common/src/main/java/immersive_aircraft/client/render/entity/renderer/VehicleEntityRenderer.java
common/src/main/java/immersive_aircraft/client/render/entity/renderer/bullet/BulletEntityRenderer.java
common/src/main/java/immersive_aircraft/client/render/entity/renderer/bullet/TinyTNTRenderer.java
common/src/main/java/immersive_aircraft/client/render/entity/renderer/utils/BBModelRenderer.java
common/src/main/java/immersive_aircraft/client/render/entity/renderer/utils/DeferredRenderBuffer.java
common/src/main/java/immersive_aircraft/cobalt/registration/CobaltFuelRegistry.java
common/src/main/java/immersive_aircraft/entity/VehicleEntity.java
common/src/main/java/immersive_aircraft/mixin/client/GameRendererMixin.java
common/src/main/java/immersive_aircraft/mixin/client/KeyboardHandlerMixin.java
common/src/main/java/immersive_aircraft/mixin/client/LivingEntityRendererMixin.java
common/src/main/java/immersive_aircraft/network/s2c/AircraftDataMessage.java
gradle.properties
gradle/controller-dependencies.gradle
neoforge/build.gradle
neoforge/src/main/java/immersive_aircraft/neoforge/ClientNeoForge.java
neoforge/src/main/java/immersive_aircraft/neoforge/NeoForgeBusEvents.java
neoforge/src/main/java/immersive_aircraft/neoforge/cobalt/network/NetworkHandlerImpl.java
neoforge/src/main/java/immersive_aircraft/neoforge/cobalt/registration/CobaltFuelRegistryImpl.java
neoforge/src/main/resources/pack.mcmeta
neoforge/src/main/templates/META-INF/neoforge.mods.toml
settings.gradle
```

New: this checkpoint, gradle/prepare-local-dependencies.py, neoforge/src/test/java/immersive_aircraft/neoforge/Local263FuelTest.java. Complete final list is exported to local-26.3-jars/evidence/ImmersiveAircraft-source-changes.txt.
