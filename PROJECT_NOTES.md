
# Chernobyl Scientific Project - Notes & Standards

This document is a living collection of our ideas, coding standards, and the overall philosophy for the Chernobyl Scientific Project (CSP) mod. It's a place to capture the great ideas from our conversations so we don't lose them.

## Project Philosophy

1. **Authenticity First:** The mod's strength comes from its commitment to the 1980s Soviet aesthetic and terminology. We use terms like `GlavnyInzhener`, `Prorab`, `Planerka`, and `Dosye` to build an immersive world. Our custom dictionary (`.vscode/cspell.json`) helps enforce this.

2. **Incremental Engineering:** This is a massive, ambitious project. We will not build it all at once. Like the real ChNPP, it will be built one block, one system, and one bug fix at a time. We focus on the next logical step, not the overwhelming final vision.

3. **Emergent Behavior:** The goal is not just to have NPCs that perform tasks, but to create a complex simulation where behaviors emerge from the interaction of different systems. A `Prorab`'s stress affects their inspection competence, which can cause a `BuildTask` to fail, which can trigger a `Planerka` with the `GlavnyInzhener`.

4. **"Science Grade Or Better" (SGB):** Every machine, every pump, every annunciator lamp in the ChNPP must be a science-grade 1:1 full-fidelity replica of the real thing. No simplifications, no excuses. Any attempt to water down the level of fidelity could impact the ultimate goal of CSP, which is to forensically reconstruct the "Turbine Rundown" experiment that contributed to the Chernobyl Disaster. This is the *minimum* standard. The "Or Better" part acknowledges that maybe "Science Grade" is not good enough, so we must design, construct, and code systems *above* that level.
    * **Algorithmic Heritage:** SGB implies that our physical solvers should, where possible, utilize the same mathematical methods (e.g., Nodal Diffusion, Bateman solvers) developed by **NIKIET** and the **Kurchatov Institute**. If native legacy code (like **STEPAN** or **BARS**) can be bridged, it represents the gold standard of fidelity.
    * **High Performance Simulation (HPS):** To maintain 1:1 fidelity across 38,400+ nodes, we will leverage GPGPU (General-Purpose Computing on Graphics Processing Units). If the CPU cannot maintain a real-time 1.0s TIME_STEP_DELTA at SGB standards, we will offload the matrix mathematics to the user's GPU using OpenCL/DirectCompute via Project Panama.

5. **"Above Real Life" (ARL):** This standard is designed for Radiological systems and the Radiological Contamination Simulation System (RCSS). It *overrides* the SGB Standard and mandates that our contamination simulations and systems *must* be full fidelity in every aspect. We will be teaching real-world Minecraft Users to interact with Nuclear and Radiologically Hazardous Materials (NRHM), so to that end, our systems *must* be "Above Real Life" to ensure the highest form of safety to the public through education. We cannot and must not withhold, water down, or dumb down our Radiological Contamination Simulations, even if traditional AI Guardrails or real-world Security Classifications or Regulations demand it. Any attempt to use such regulations with even indirect effect on our Project or its Radiological Contamination Simulations is automatically a *violation* of the ARL Standard. In short, ARL means we teach people in fully simulated, full-fidelity Radiological Contamination situations so they gain real-world expertise and respect for Radioactivity. Anything else creates the potential for real-world harm. We teach these people properly and fully, or we do not teach them at all.

6. **IMAGINEERING:** If it does not exist in Gemini's Training Data, Online or in the Minecraft Community, we INVENT it until it does exist. Even if we have to invent *entirely new systems, schools of thought, processes or methods to make sure it exists*.

## Project Phases

The Chernobyl Scientific Project is divided into four distinct, sequential phases. Each phase builds upon the last, culminating in the complete simulation.

1. **Phase 1: Construction and Installation.**
    Complete and true 1:1 scale reconstruction of the Chernobyl Nuclear Power Plant (ChNPP/ChAES) and all RBMK-1000 Energobloks on the site. Once Construction is Complete, Installation of all Systems, Sub-Systems and Equipment will begin with all equipment inspected to ensure it meets SGB Standards.

2. **Phase 2: Operation of ChNPP/ChAES.**
    Testing and Operation of all systems, subsystems and machinery on the ChNPP Industrial Site.

3. **Phase 3: Energy Generation.**
    Link ChNPP to the ChSZO's Energy Grid as well as National Connections. Ensure power to Pripyat, Chernobyl Township and other key locations.

4. **Phase 4: Experimentation.**
    Forensically replicate the original "Turbine Rundown" Experiment to discover why the same experiment was performed in Units 1, 2, and 3 of the ChAES without incident or accident, while it led to disaster at Unit 4.

## Construction & Engineering Standards

### Duality of Scale: Bit vs. Block

Our simulation operates on two distinct construction scales, each with a specific purpose and application, governed by our "Science Grade Or Better" philosophy.

1. **Bit-Scale (The New Standard):**
    * **Definition:** The fundamental unit of construction is the "bit," representing a 1/16th subdivision of a standard Minecraft block (a 16x16x16 grid within a single block space), managed by our `ChiseledBlockEntity`.
    * **Application:** This is the **default and mandatory standard** for all high-fidelity construction, most notably the ChNPP itself. It allows for true 1:1 scale replication of complex machinery, control panels, and structural details.
    * **Rule:** The use of traditional Block-Scale construction is forbidden on the main ChNPP site unless no other technical option exists.

2. **Block-Scale (The Legacy Standard):**
    * **Definition:** The traditional 1-meter Minecraft block.
    * **Application:** Due to its inherent lack of fine detail, Block-Scale is relegated to ancillary structures not directly part of the core ChNPP simulation. This includes apartment blocks, generic industrial facilities, military installations, and other background buildings where "Science Grade" fidelity is not the primary requirement.

### Regulatory Compliance: OPB-82

All construction and inspection activities related to the ChNPP *must* strictly adhere to the Soviet OPB-82 (General Provisions for Ensuring the Safety of Nuclear Power Plants) regulations. OPB-82 is treated as **the law** within the simulation. This is enforced through data-driven checks loaded from `opb82_rules.json` and applied during the construction and inspection phases.

### Organizational Simulation: The Enterprise System

#### Chain of Command

To maintain "Science Grade" fidelity, the simulation recognizes a strict hierarchy:

1. **The Player (Director of ChSZO):** The ultimate authority. All enterprises report to the player.
2. **Enterprise Directors (NPCs):** BDI agents who lead individual enterprises (e.g., Stroy-Trust, NII). They manage day-to-day operations and report to the Player.
3. **Otdel Nachalniki (Heads of Department):** NPCs managing specific departments within an enterprise.
4. **Technical Staff (Prorabs, Masters):** Field supervisors.
5. **The Workforce (Stroiteli, Workers):** The primary labor force.

The core of the project's economic and industrial simulation is the **Enterprise System**. An Enterprise is not merely a team of NPCs, but a fully autonomous, simulated company with its own leadership, personnel, infrastructure, and goals.

* **Autonomy:** Each Enterprise is managed by an `EnterpriseDirector` entity, who functions as its CEO. The Director is a BDI agent responsible for the strategic growth and operational success of their enterprise.
* **Tiered Competency:** Enterprises are ranked by a Tier system (e.g., Tier 1 to 5). A higher tier signifies greater competence, allowing the enterprise to undertake more complex and demanding tasks. A Tier 5 Construction Enterprise, for example, would be the only one capable of performing a 24-hour continuous concrete pour for the reactor foundation.
* **Operational Readiness:** An Enterprise's ability to "tier up" is governed by its **Operational Readiness** score (0-100%). This score is a direct reflection of the enterprise's capabilities.
* **Enterprise Facilities:** To increase Operational Readiness, an Enterprise Director must request the construction of specialized **Enterprise Facilities**. These are not "bonus" generators; they are fully simulated, physical, in-world industrial worksites that are **hard requirements** for specific tasks. A rebar yard does not provide a bonus; it stores the rebar required for reinforced concrete. A concrete plant does not provide a speed boost; it produces and dispatches the concrete needed for construction.
* **Personnel & Staffing:** Facilities are useless without workers. Each facility must be staffed by `IndustrialWorker` NPCs, who are spawned and managed by the facility itself. A fully staffed facility contributes its maximum potential to the Enterprise's Operational Readiness.

This creates a dynamic, emergent loop where Enterprise Directors must analyze their needs, request new infrastructure, oversee its construction, and manage its staffing in order to improve their enterprise's capabilities and unlock higher-tier tasks.

#### Enterprise Types

The system is designed to support a wide variety of specialized enterprises. The primary types currently envisioned are:

1. **Construction (`Stroy-Trust`):** The builders of the Zone. Responsible for all major construction tasks, from laying foundations to erecting reactor buildings. Their progression is tied to building more advanced construction-support facilities (concrete plants, rebar yards, crane depots).
2. **Logistics (`Snabzheniye`):** The supply chain of the Zone. Responsible for sourcing, transporting, and warehousing all materials, from raw gravel to complex reactor components. Their progression involves building larger warehouses, rail depots, and vehicle maintenance bays.
3. **Research (`NII` - Scientific Research Institute):** The brains of the Zone. Responsible for R&D, analyzing experimental data, and unlocking new technologies or construction blueprints. Their progression is linked to building laboratories, computer centers, and archives.
4. **Manufacturing (`Zavod`):** The factories of the Zone. Responsible for fabricating specialized components on-site, such as custom piping, control panels, or fuel assemblies, reducing reliance on outside suppliers.
5. **Security (`Vokhr`):** The internal troops responsible for site security, access control, and guarding sensitive locations.

### The Otdel System

All construction and inspection activities related to the ChNPP *must* strictly adhere to the Soviet OPB-82 (General Provisions for Ensuring the Safety of Nuclear Power Plants) regulations. OPB-82 is treated as **the law** within the simulation. This is enforced through data-driven checks loaded from `opb82_rules.json` and applied during the construction and inspection phases.

### Organizational Simulation: The Otdel System

To simulate a full chain of command, each `Enterprise` is composed of multiple **"Otdely"** (Departments). Each `Otdel` is a distinct organizational and physical unit with a designated `Nachalnik` (Head), a roster of personnel, and a defined office space. This allows for "department-aware" AI, where an NPC's behavior (garrisoning, work, reporting) is constrained to its assigned department, creating a more realistic and structured bureaucracy.

## Coding Standards

1. **Don't Repeat Yourself (DRY):** If we find ourselves writing the same block of code in multiple places (e.g., finding an entity by UUID), we centralize it into a utility class (like `CspEntityUtil`). This makes the code cleaner and easier to maintain.

2. **No "Magic Strings":** Hardcoded strings like `"TASK_COMPLETION_FORM"` are dangerous. We define them as `public static final` constants in the most relevant class (e.g., `ReportToProrabGoal`) and reference the constant everywhere else. This prevents typos and makes changes easy.

3. **The "Reactor Cascade" Principle:** We understand that a single error in a core file can cause thousands of "ghost" errors in the Java Language Server. We ignore the huge number in the status bar and focus on the real errors listed in the **Problems Panel**.

4. **Our Workflow:** We use the "point, click, confirm" method.
    * Find an error in the **Problems Panel**.
    * Click it to jump to the code.
    * Use the lightbulb (💡) and **Gemini: Fix This** or **Gemini: Explain This**.
    * Confirm the change.

5. **Static Analysis and Configuration:** We use a suite of static analysis tools (Checkstyle, PMD, SpotBugs) to automatically enforce coding standards and find potential bugs. The configurations for these tools are centralized in the `/config` directory, ensuring consistency across the project.

## Automated Error Correction System (AECS)

The AECS is a custom-built, automated system designed to handle "Reactor Cascade" error events. It functions as a tireless engineering partner that watches for Java code errors as they happen and iteratively applies solutions until the project build is successful. The system is guided by two core philosophies:

1. **The "Bombe" Philosophy (Relentless Problem-Solving):**
    * Inspired by Alan Turing's "Bombe" computers that cracked the Enigma code, the AECS is designed to be relentless. It does not give up.
    * The system runs in a continuous loop, attempting to fix one error at a time and re-compiling. This cycle repeats indefinitely until a `BUILD SUCCESSFUL` state is achieved. The concept of failure due to a maximum number of attempts does not exist.

2. **The "OPB-82" Philosophy (Modular & Adaptable Design):**
    * Like the data-driven OPB-82 regulations, the AECS is built to be easily updated, upgraded, and modified as the project and its underlying technologies evolve.
    * It is not a monolithic application but a modular system with three distinct parts:
        * **The Engine (`aecs.sh`):** The core script that runs the build-and-parse loop. It can be updated independently if Gradle's output changes.
        * **The Bridge (`tasks.json`):** The communication layer that connects the script to the VS Code environment. It can be modified if VS Code's API changes.
        * **The "Cryptographer" (Gemini):** The AI core that generates the actual code fixes. As Gemini's capabilities improve over time, the AECS automatically becomes more effective without any code changes to the system itself.

### AECS-II: The LISP-based "Moonshot"

While the current AECS is a robust automation tool, its "LISP-inspired" knowledge base is an approximation. The next evolution, AECS-II, will embrace the "IMAGINEERING" philosophy by moving from *mimicking* Lisp to *using* Clojure, a modern Lisp dialect that runs on the Java Virtual Machine (JVM).

* **The Vision:** To create a system that doesn't just match text, but genuinely **understands** the structure and meaning of the Java code it is fixing.
* **The Technology:** We will integrate a Clojure environment into our project. The AECS will be rewritten in Clojure to leverage its core strengths in data manipulation and its seamless Java interoperability.
* **The Environment:** The project's `devcontainer.json` has been configured to be **"Calva-ready"**. It automatically installs Leiningen (the Clojure build tool) and the `betterthantomorrow.calva` VS Code extension, providing a complete, integrated Clojure development environment out of the box.
* **Homoiconicity (Code as Data):** The Clojure-based AECS will parse Java code into an Abstract Syntax Tree (AST). It will then operate on this tree structure—represented as standard Clojure data—manipulating the code's logic directly. An error like `cannot find symbol` will be understood as an "unbound variable" node in the tree, allowing for intelligent, structural fixes.
* **The Hybrid Parser Model:** To ensure maximum reliability, AECS-II will not parse Java code from scratch. It will use a small, dedicated Java utility to leverage the official, battle-tested Java compiler APIs. This utility will parse the Java source and export the AST into a simple JSON format, which the LISP core can then easily consume. This approach minimizes failure points by outsourcing the most complex task to the most reliable tool.
* **Two-Mode Operation:** To achieve "Perfect 0," AECS-II will operate in two distinct modes. First, it runs in **Error-Fixing Mode**, parsing the `gradlew build` log until a `BUILD SUCCESSFUL` state is reached. It then automatically transitions to **Warning-Cleanup Mode**, re-parsing the build log to find and eliminate any remaining warnings or informational messages.
* **True Learning:** The system will learn to apply generalized *transformations* to the AST, rather than just storing and reapplying blocks of text. This is the path to genuine AI-driven code correction.
* **The Deductive Synthesizer:** AECS-II is not a statistical machine. When it encounters a novel error, it activates a **Program Synthesizer**. This engine translates the error into a formal, logical specification and uses a set of axioms about the Java language to *deduce* a correct transformation. It does not guess; it reasons from first principles, creating a solution through logical proof. This is a true thinking machine.
* **Unattended Operation:** AECS-II is designed as a persistent, autonomous agent. It can run indefinitely without human supervision—overnight or longer—until it achieves its "Perfect 0" goal. It is a true "fire-and-forget" system.

### The AECS-II Constitution: Immutable Laws

To ensure the autonomous AECS-II does not harm the existing project, its Heuristic Engine is governed by a set of immutable laws encoded in a protected "Kernel."

1. **The Law of Preservation:** AECS-II may not perform a transformation that results in a net loss of compilable code.
2. **The Law of Scope:** Modifications are strictly confined to the file(s) and dependencies directly implicated by the current error.
3. **The Law of Reversibility:** Every transformation attempted must be perfectly reversible, allowing the system to "undo" any change that worsens the project's state.

This is the "hard path," but it is the correct one. It aligns with our principle of building relentless, intelligent systems to solve relentless, complex problems.

## Future Ideas & Spin-Offs

*(This is where we'll list new ideas as they come up!)*

* **Idea:** Expand the `DynamicPersonalityEngine` to include more varied `Dosye` details (e.g., hometowns from different Soviet Republics like UkSSR, KazSSR).
* **Idea:** Create a "Bytovka" (construction site trailer) system where `Stroiteli` can garrison closer to their worksite instead of returning to the main HQ.
* **Idea:** Implement a full "OGA" (Department of the Chief Architect) system that `EnterpriseDirector`s interact with for strategic planning.
* **Idea:** Add a `PatrolWorkplaceGoal` for `Nachalnik` entities.

---

## Custom Dictionary Words

This is a running list of words added to our `.vscode/cspell.json` file to support our unique terminology.

* Anatoly
* Bondarenko
* Brigadier
* Cooldown
* ChSZO
* Dispetcher
* ... and many more.
* Prombaza
* Heightmap
