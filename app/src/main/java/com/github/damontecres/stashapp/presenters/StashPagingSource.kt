package com.github.damontecres.stashapp.presenters

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.exception.ApolloException
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.createApolloClient
import com.github.damontecres.stashapp.data.CountAndList

/**
 * A PagingSource for Stash
 *
 * @property context
 * @property pageSize how many items per page
 * @property dataSupplier how to query and parse data
 */
class StashPagingSource<T : Query.Data, D : Any>(
    private val context: Context,
    private val pageSize: Int,
    private val dataSupplier: DataSupplier<T, D>
) :
    PagingSource<Int, D>() {

    interface DataSupplier<T : Query.Data, D : Any> {
        /**
         * Create query with the given filter
         *
         * @param filter the filter to use
         */
        fun createQuery(filter: FindFilterType?): Query<T>

        /**
         * Parse the data returned from the query created by [createQuery]
         *
         * @param data the Query's data object
         * @return The list of data along with the total count
         */
        fun parseQuery(data: T?): CountAndList<D>

        /**
         * Get the default filter
         *
         * By default, this sorts by name ascending
         */
        fun getDefaultFilter(): FindFilterType {
            return FindFilterType(
                sort = Optional.present("name"),
                direction = Optional.present(SortDirectionEnum.ASC)
            )
        }
    }

    private suspend fun fetchPage(page: Int): CountAndList<D> {
        val apolloClient = createApolloClient(context)
        if (apolloClient != null) {
            val filter = dataSupplier.getDefaultFilter().copy(
                per_page = Optional.present(pageSize),
                page = Optional.present(page),
            )
            val query = dataSupplier.createQuery(filter)
            val results = apolloClient.query(query).execute()
            return dataSupplier.parseQuery(results.data)
        }
        return CountAndList(-1, listOf())
    }

    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, D> {
        try {
            // Start refresh at page 1 if undefined.
            val pageNum = (params.key ?: 1).toInt()
            val results = fetchPage(pageNum)
            if (results.count < 0) {
                return LoadResult.Error(RuntimeException("Invalid count"))
            }
            // If the total fetched results is less than the total number of items, then there is a next page
            val nextPageNum = if (pageSize * pageNum < results.count) pageNum + 1 else null

            return LoadResult.Page(
                data = results.list,
                prevKey = if (pageNum > 1) pageNum - 1 else null, // Only a previous page if current page is 2+
                nextKey = nextPageNum
            )
        } catch (e: ApolloException) {
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, D>): Int? {
        // Try to find the page key of the closest page to anchorPosition from
        // either the prevKey or the nextKey; you need to handle nullability
        // here.
        //  * prevKey == null -> anchorPage is the first page.
        //  * nextKey == null -> anchorPage is the last page.
        //  * both prevKey and nextKey are null -> anchorPage is the
        //    initial page, so return null.
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
