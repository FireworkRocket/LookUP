package org.fireworkrocket.lookup.processor;

import java.util.List;

/**
 * JsonData 类，用于处理 JSON 数据。
 */
class JsonData {
    private String status;
    private String code;
    private String error;
    private String success;
    private List<JsonData.Data> data;
    private String url;
    private List<String> pics;
    private List<String> pic;
    private String imgurl;
    private String imageUrl;
    private String acgurl;

    /**
     * 获取状态。
     *
     * @return 状态字符串
     */
    String getStatus() {
        if (status != null && !status.isEmpty()) {
            return status;
        } else if (code != null && !code.isEmpty()) {
            return code;
        } else if (error != null && !error.isEmpty()) {
            return error;
        } else if (success != null && !success.isEmpty()) {
            return success;
        } else {
            return null;
        }
    }

    /**
     * 获取数据列表。
     *
     * @return 数据列表
     */
    List<JsonData.Data> getData() {
        return data;
    }

    /**
     * 获取 URL。
     *
     * @return URL 字符串
     */
    String getUrl() {
        if (url != null && !url.isEmpty()) {
            return url;
        } else if (pics != null && !pics.isEmpty()) {
            return String.join(" $n ", pics);
        } else if (imgurl != null && !imgurl.isEmpty()) {
            return imgurl;
        } else if (pic != null && !pic.isEmpty()) {
            return String.join(" $n ", pic);
        } else if (imageUrl != null && !imageUrl.isEmpty()) {
            return imageUrl;
        } else if (acgurl != null && !acgurl.isEmpty()) {
            return acgurl;
        } else {
            for (Data data : getData()) {
                if (data.getOriginalUrl() != null && !data.getOriginalUrl().isEmpty()) {
                    return data.getOriginalUrl();
                } else if (data.getUrl() != null && !data.getUrl().isEmpty()) {
                    return data.getUrl();
                }
            }
            return null;
        }
    }

    /**
     * Data 类，用于存储数据。
     */
    static class Data {
        private String pid;
        private String page;
        private String author;
        private String title;
        private boolean r18;
        private String uploadDate;
        private List<String> tags;
        private Urls urls;
        private String url;
        private int width;
        private int height;
        private String ext;

        /**
         * 获取 PID。
         *
         * @return PID 字符串
         */
        public String getPid() {
            return pid;
        }

        /**
         * 获取页面。
         *
         * @return 页面字符串
         */
        public String getPage() {
            return page;
        }

        /**
         * 获取作者。
         *
         * @return 作者字符串
         */
        public String getAuthor() {
            return author;
        }

        /**
         * 获取 R18 值。
         *
         * @return R18 布尔值
         */
        public boolean getR18Vel() {
            return r18;
        }

        /**
         * 获取标题。
         *
         * @return 标题字符串
         */
        public String getTitle() {
            return title;
        }

        /**
         * 获取上传日期。
         *
         * @return 上传日期字符串
         */
        public String getUploadDate() {
            return uploadDate;
        }

        /**
         * 获取标签列表。
         *
         * @return 标签列表
         */
        public List<String> getTags() {
            return tags;
        }

        /**
         * 获取原始 URL。
         *
         * @return 原始 URL 字符串
         */
        public String getOriginalUrl() {
            return urls != null ? urls.getOriginal() : null;
        }

        /**
         * 获取分辨率。
         *
         * @return 分辨率字符串
         */
        public String getRes() {
            return width + "x" + height;
        }

        /**
         * 获取扩展名。
         *
         * @return 扩展名字符串
         */
        public String getExt() {
            return ext;
        }

        /**
         * 获取 URL。
         *
         * @return URL 字符串
         */
        public String getUrl() {
            return url;
        }

        /**
         * Urls 类，用于存储 URL。
         */
        static class Urls {
            private String original;

            /**
             * 获取原始 URL。
             *
             * @return 原始 URL 字符串
             */
            public String getOriginal() {
                return original;
            }
        }
    }
}