package com.arata.yukarilauncher.feature.download.item

import com.arata.yukarilauncher.feature.download.enums.Category
import com.arata.yukarilauncher.feature.download.enums.Classify
import com.arata.yukarilauncher.feature.download.enums.Platform
import java.util.Date

/**
 * 基础的信息类
 * @param classify 该项目的类别
 * @param platform 该项目的所属平台
 * @param projectId 该项目的唯一标识
 * @param slug 该项目的slug
 * @param author 该项目的作者
 * @param title 该项目的标题
 * @param description 该项目的描述
 * @param downloadCount 该项目的总下载量
 * @param uploadDate 该项目的上传日期
 * @param iconUrl 该项目的封面链接
 * @param category 该项目的标签
 */
open class InfoItem(
    val classify: Classify,
    val platform: Platform,
    val projectId: String,
    val slug: String,
    val author: Array<String>?,
    val title: String,
    val description: String,
    val downloadCount: Long,
    val uploadDate: Date,
    val iconUrl: String?,
    val category: List<Category>
) {
    fun copy() = InfoItem(
        classify, platform, projectId, slug, author, title, description, downloadCount, uploadDate, iconUrl, category
    )

    override fun toString(): String {
        return "InfoItem(" +
                "classify='$classify', " +
                "platform='$platform', " +
                "projectId='$projectId', " +
                "slug='$slug', " +
                "author=${author.contentToString()}, " +
                "title='$title', " +
                "description='$description', " +
                "downloadCount=$downloadCount, " +
                "uploadDate=$uploadDate, " +
                "iconUrl='$iconUrl', " +
                "category=$category" +
                ")"
    }
}