# Code Rules for Book's Story (book-story)

This rule file specifies the coding guidelines, project structure, technology stack, and functional programming standards that must be followed when contributing to the `book-story` codebase.

---

## 1. Technology Stack
*   **Language:** Kotlin (100% Kotlin codebase).
*   **UI Toolkit:** Jetpack Compose (Material Design 3 with Material You dynamic system color matching).
*   **Architecture:** Clean Architecture (Core, Data, Domain, Presentation) combined with Unidirectional Data Flow (UDF).
*   **Dependency Injection:** Dagger Hilt for ViewModel and repository lifecycle management.
*   **Database & Cache:** Room Database (relational SQLite persistence) & Datastore Preferences (asynchronous settings cache).
*   **Asynchronous Processing:** Kotlin Coroutines and cold/hot Streams (Flow, StateFlow, SharedFlow).
*   **Parsers:** PDFBox-Android (PDF document parsing), JSoup (HTML parsing for EPUB contents).

---

## 2. Project Source Structure
Maintain the Clean Architecture layering package hierarchy under `ua.acclorite.book_story`:
*   `core/` ➔ Reusable helpers, custom logger, language settings, and global UI definitions.
*   `data/` ➔ Database initialization, entity mappings, shared preferences, and repository implementations.
*   `domain/` ➔ Pure domain models, repository interfaces, and use-case interactors (represented as single-purpose functional classes using `operator fun invoke`).
*   `presentation/` ➔ UI screens structured in UDF fashion (e.g., `Screen.kt`, `ViewModel/Model.kt`, `State.kt`, `Event.kt`).
*   `ui/` ➔ Shared layout composables, text styles, icons, and theme definitions.

---

## 3. Immutability by Default
*   **Data Models:** Always use `data class` with immutable variables (`val`) instead of mutable ones (`var`).
*   **Compose States:** Annotate UI state models with `@Immutable` or `@Stable` to allow Compose to skip redundant recompositions.
*   **State Copying:** Use the `.copy()` function when modifying properties on a class instance. Never perform direct reassignments.
    *   *Correct:* `val newState = state.copy(isLoading = false)`
    *   *Incorrect:* `state.isLoading = false`

---

## 2. Unidirectional Data Flow (UDF) & MVI Patterns
*   **UI Events:** Model all user interactions, navigations, and callbacks as subclasses of a single `sealed class` (e.g. `LibraryEvent`). This ensures compile-time safety and exhaustiveness inside the ViewModel.
*   **ViewModel State:** State must be exposed as a read-only stream:
    ```kotlin
    private val _state = MutableStateFlow(LibraryState())
    val state = _state.asStateFlow()
    ```
*   **Event Reducer:** Direct all incoming actions to a single `onEvent(event: LibraryEvent)` handler. Modify state inside it using functional state updates:
    ```kotlin
    _state.update { it.copy(searchQuery = event.query) }
    ```

---

## 3. Monadic Result Handling
*   **Avoid Throwing Exceptions:** Do not throw standard custom exceptions for business-flow errors. Return a `Result<T>` instead.
*   **Avoid Imperative Try-Catch:** Avoid raw `try-catch` blocks inside use cases or repositories. Chain results using monadic handlers:
    *   *Correct:*
        ```kotlin
        repository.addBook(book).fold(
            onSuccess = { logI("Inserted") },
            onFailure = { logE("Failed: ${it.message}") }
        )
        ```
    *   *Incorrect:*
        ```kotlin
        try {
            repository.addBook(book)
        } catch (e: Exception) {
            logE(e)
        }
        ```

---

## 4. Functional Collection & Stream Processing
*   **Standard Library Operators:** Avoid `for` loops or index-based traversals. Prefer standard functional operators: `.map()`, `.filter()`, `.any()`, `.associateBy()`, `.flatMap()`.
*   **Scope Functions:** Use scope functions (`let`, `run`, `apply`, `also`, `takeIf`) to construct fluid, expressions-based transformations instead of nesting multiple `if` conditions.

---

## 5. Pure Functions & Higher-Order Functions
*   **Use Cases:** Each UseCase class must serve a single purpose and be declared using `operator fun invoke(...)`. Ensure it represents a pure function returning a deterministic output based on inputs.
*   **Abstract Callbacks:** When writing UI filters or sorting logics, accept lambda function signatures (e.g. `selector: (T) -> Comparable<*>?`) as parameters to build higher-order functions.
