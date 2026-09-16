/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import ua.acclorite.book_story.core.helpers.compareByWithOrder
import ua.acclorite.book_story.domain.model.library.Category
import ua.acclorite.book_story.presentation.library.LibraryEvent
import ua.acclorite.book_story.presentation.library.model.AllBooksFilter
import ua.acclorite.book_story.presentation.library.model.LibraryLayout
import ua.acclorite.book_story.presentation.library.model.LibrarySortOrder
import ua.acclorite.book_story.presentation.library.model.LibraryTitlePosition
import ua.acclorite.book_story.presentation.library.model.SelectableBook
import ua.acclorite.book_story.ui.theme.DefaultTransition

@Composable
fun LibraryPager(
    books: List<SelectableBook>,
    allBooksFilter: AllBooksFilter = AllBooksFilter.ALL,
    tagsStatusFilter: AllBooksFilter = AllBooksFilter.ALL,
    selectedTag: String? = null,
    pagerState: PagerState,
    categories: List<Category>,
    showDefaultCategory: Boolean,
    perCategorySort: Boolean,
    sortOrder: LibrarySortOrder,
    sortOrderDescending: Boolean,
    layout: LibraryLayout,
    gridSize: Int,
    autoGridSize: Boolean,
    hasSelectedItems: Boolean,
    titlePosition: LibraryTitlePosition,
    readButton: Boolean,
    showProgress: Boolean,
    isLoading: Boolean,
    isRefreshing: Boolean,
    selectBook: (LibraryEvent.OnSelectBook) -> Unit,
    onAllBooksFilterChange: (LibraryEvent.OnAllBooksFilterChange) -> Unit,
    onTagsStatusFilterChange: (LibraryEvent.OnTagsStatusFilterChange) -> Unit,
    onTagFilterChange: (LibraryEvent.OnTagFilterChange) -> Unit,
    navigateToBrowse: (LibraryEvent.OnNavigateToBrowse) -> Unit,
    navigateToBookInfo: (LibraryEvent.OnNavigateToBookInfo) -> Unit,
    navigateToReader: (LibraryEvent.OnNavigateToReader) -> Unit,
) {
    val categorizedBooks = remember(
        books,
        categories,
        perCategorySort,
        sortOrder,
        sortOrderDescending,
        showDefaultCategory,
        allBooksFilter,
        tagsStatusFilter,
        selectedTag
    ) {
        fun List<SelectableBook>.sortBooks(
            librarySortOrder: LibrarySortOrder,
            librarySortOrderDescending: Boolean
        ): List<SelectableBook> {
            return sortedWith(
                compareByWithOrder(librarySortOrderDescending) { book ->
                    when (librarySortOrder) {
                        LibrarySortOrder.NAME -> book.data.title.trim()
                        LibrarySortOrder.LAST_READ -> book.data.lastOpened
                        LibrarySortOrder.PROGRESS -> book.data.progress
                        LibrarySortOrder.AUTHOR -> book.data.author.getAsString()
                    }
                }
            )
        }

        derivedStateOf {
            val categorizedBooks = mutableListOf<List<SelectableBook>>()

            // 1. Process Tab (id = -1)
            val processCategory = categories.find { it.id == -1 }
            val inProcessBooks = books.filter {
                (it.data.progress > 0f || it.data.lastOpened != null) && it.data.progress < 1f
            }
            categorizedBooks.add(
                if (perCategorySort && processCategory != null) {
                    inProcessBooks.sortBooks(
                        processCategory.sortOrder,
                        processCategory.sortOrderDescending
                    )
                } else {
                    inProcessBooks
                }
            )

            // 2. Tags Tab (id = -4)
            val tagsCategory = categories.find { it.id == -4 }
            val filteredTagsBooks = books.filter { book ->
                val matchesTag = if (selectedTag.isNullOrBlank()) {
                    true
                } else {
                    book.data.tags.any { it.equals(selectedTag, ignoreCase = true) }
                }
                val matchesStatus = when (tagsStatusFilter) {
                    AllBooksFilter.ALL -> true
                    AllBooksFilter.NOT_STARTED -> book.data.progress == 0f && book.data.lastOpened == null
                    AllBooksFilter.PROCESSING -> (book.data.progress > 0f || book.data.lastOpened != null) && book.data.progress < 1f
                    AllBooksFilter.COMPLETED -> book.data.progress >= 1f
                }
                matchesTag && matchesStatus
            }
            categorizedBooks.add(
                if (perCategorySort && tagsCategory != null) {
                    filteredTagsBooks.sortBooks(
                        tagsCategory.sortOrder,
                        tagsCategory.sortOrderDescending
                    )
                } else {
                    filteredTagsBooks
                }
            )

            // 3. All Book Tab (id = -2)
            val allBooksCategory = categories.find { it.id == -2 }
            val filteredAllBooks = when (allBooksFilter) {
                AllBooksFilter.ALL -> books
                AllBooksFilter.NOT_STARTED -> books.filter {
                    it.data.progress == 0f && it.data.lastOpened == null
                }
                AllBooksFilter.PROCESSING -> books.filter {
                    (it.data.progress > 0f || it.data.lastOpened != null) && it.data.progress < 1f
                }
                AllBooksFilter.COMPLETED -> books.filter {
                    it.data.progress >= 1f
                }
            }
            categorizedBooks.add(
                if (perCategorySort && allBooksCategory != null) {
                    filteredAllBooks.sortBooks(
                        allBooksCategory.sortOrder,
                        allBooksCategory.sortOrderDescending
                    )
                } else {
                    filteredAllBooks
                }
            )

            // 4. Completed Tab (id = -3)
            val completedCategory = categories.find { it.id == -3 }
            val completedBooks = books.filter { it.data.progress >= 1f }
            categorizedBooks.add(
                if (perCategorySort && completedCategory != null) {
                    completedBooks.sortBooks(
                        completedCategory.sortOrder,
                        completedCategory.sortOrderDescending
                    )
                } else {
                    completedBooks
                }
            )

            // 5. Custom Categories (id > 0)
            categories
                .filter { it.id > 0 }
                .sortedBy { it.order }
                .forEach { category ->
                    val catBooks = books.filter { book ->
                        book.data.categories.any { it == category.id }
                    }
                    categorizedBooks.add(
                        if (perCategorySort) {
                            catBooks.sortBooks(
                                category.sortOrder,
                                category.sortOrderDescending
                            )
                        } else {
                            catBooks
                        }
                    )
                }

            return@derivedStateOf categorizedBooks.let {
                if (!perCategorySort) {
                    return@let it.map { books ->
                        books.sortBooks(
                            sortOrder,
                            sortOrderDescending
                        )
                    }
                }

                return@let it
            }
        }
    }

    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
        val category = remember(categorizedBooks, index) {
            derivedStateOf {
                categorizedBooks.value.getOrElse(index) { emptyList() }
            }
        }

        val isTagsTab = index == 1
        val isAllBooksTab = index == 2

        val allAvailableTags = remember(books) {
            books.flatMap { it.data.tags }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .sortedWith(String.CASE_INSENSITIVE_ORDER)
        }

        val content = @Composable {
            Box(modifier = Modifier.fillMaxSize()) {
                DefaultTransition(visible = !isLoading) {
                    LibraryLayout(
                        books = category.value,
                        gridSize = gridSize,
                        autoGridSize = autoGridSize,
                        layout = layout
                    ) { book ->
                        LibraryItem(
                            book = book,
                            layout = layout,
                            hasSelectedItems = hasSelectedItems,
                            titlePosition = titlePosition,
                            readButton = readButton,
                            showProgress = showProgress,
                            selectBook = { select ->
                                selectBook(
                                    LibraryEvent.OnSelectBook(
                                        id = book.data.id,
                                        select = select
                                    )
                                )
                            },
                            navigateToBookInfo = {
                                navigateToBookInfo(
                                    LibraryEvent.OnNavigateToBookInfo(
                                        book.data.id
                                    )
                                )
                            },
                            navigateToReader = {
                                navigateToReader(
                                    LibraryEvent.OnNavigateToReader(
                                        book.data.id
                                    )
                                )
                            }
                        )
                    }
                }

                LibraryEmptyPlaceholder(
                    isLoading = isLoading,
                    isRefreshing = isRefreshing,
                    isBooksEmpty = category.value.isEmpty(),
                    navigateToBrowse = navigateToBrowse
                )
            }
        }

        if (isTagsTab) {
            Column(modifier = Modifier.fillMaxSize()) {
                TagsFilterChips(
                    availableTags = allAvailableTags,
                    selectedTag = selectedTag,
                    selectedStatus = tagsStatusFilter,
                    onTagSelected = { tag ->
                        onTagFilterChange(LibraryEvent.OnTagFilterChange(tag))
                    },
                    onStatusSelected = { status ->
                        onTagsStatusFilterChange(LibraryEvent.OnTagsStatusFilterChange(status))
                    }
                )
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
            }
        } else if (isAllBooksTab) {
            Column(modifier = Modifier.fillMaxSize()) {
                AllBooksFilterChips(
                    selectedFilter = allBooksFilter,
                    onFilterSelected = { filter ->
                        onAllBooksFilterChange(LibraryEvent.OnAllBooksFilterChange(filter))
                    }
                )
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
            }
        } else {
            content()
        }
    }
}