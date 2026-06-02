package com.arata.yukarilauncher.utils

import kotlin.collections.AbstractMutableList

/** 配列をフィルタリング条件に基づいてラップする変更可能なサブリスト。 */
class FilteredSubList<E>(motherList: Array<E>, filter: BasicPredicate<E>) : AbstractMutableList<E>() {

    /** フィルタリング後の要素を保持する内部リスト。 */
    private val mArrayList = ArrayList<E>()

    init {
        refresh(motherList, filter)
    }

    /**
     * フィルタ条件を再適用してリストを更新する。
     * @param motherArray 元の配列
     * @param filter フィルタ条件
     */
    fun refresh(motherArray: Array<E>, filter: BasicPredicate<E>) {
        if (mArrayList.isNotEmpty()) mArrayList.clear()

        for (item in motherArray) {
            if (filter.test(item)) {
                mArrayList.add(item)
            }
        }
        mArrayList.trimToSize()
    }

    /** @return フィルタリング後の要素数 */
    override val size: Int
        get() = mArrayList.size

    /** @return フィルタリング後のイテレータ */
    override fun iterator(): MutableIterator<E> {
        return mArrayList.iterator()
    }

    /** @param element 削除する要素 @return 削除成功時は true */
    override fun remove(element: E): Boolean {
        return mArrayList.remove(element)
    }

    /** @param elements 削除するコレクション @return 変更があった場合は true */
    override fun removeAll(elements: Collection<E>): Boolean {
        return mArrayList.removeAll(elements)
    }

    /** @param elements 保持するコレクション @return 変更があった場合は true */
    override fun retainAll(elements: Collection<E>): Boolean {
        return mArrayList.retainAll(elements)
    }

    /** 全要素をクリアする。 */
    override fun clear() {
        mArrayList.clear()
    }

    /** @param index 取得する位置 @return 指定位置の要素 */
    override fun get(index: Int): E {
        return mArrayList[index]
    }

    /** @param index 削除する位置 @return 削除された要素 */
    override fun removeAt(index: Int): E {
        return mArrayList.removeAt(index)
    }

    /** @param index 追加する位置 @param element 追加する要素 */
    override fun add(index: Int, element: E) {
        mArrayList.add(index, element)
    }

    /** @param index 設定する位置 @param element 設定する要素 @return 以前の要素 */
    override fun set(index: Int, element: E): E {
        return mArrayList.set(index, element)
    }

    /** @return リストイテレータ */
    override fun listIterator(): MutableListIterator<E> {
        return mArrayList.listIterator()
    }

    /** @param index 開始位置 @return 指定位置からのリストイテレータ */
    override fun listIterator(index: Int): MutableListIterator<E> {
        return mArrayList.listIterator(index)
    }

    /** @param fromIndex 開始位置 @param toIndex 終了位置 @return 部分リスト */
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        return mArrayList.subList(fromIndex, toIndex)
    }

    /** フィルタ条件を定義するインターフェース。 */
    interface BasicPredicate<E> {
        /** @param item 判定対象 @return 条件を満たす場合は true */
        fun test(item: E): Boolean
    }
}