package com.walker.pojo;

import com.walker.annotation.NormalizeFilePath;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @Author chengQing
 * @Date 2026/3/12 9:37
 * @PackageName:com.walker.pojo
 * @ClassName: ArticleFile
 * @Description: 文章附件
 */
@Getter
@Setter
@TableName("bbs_article_file")
@ApiModel(value = "ArticleFile对象", description = "")
public class ArticleFile {
    @ApiModelProperty("文章附件id")
    @TableId(value = "file_id", type = IdType.AUTO)
    private Integer fileId;

    @ApiModelProperty("附件名称")
    @TableField("file_name")
    private String fileName;

    @ApiModelProperty("附件类型")
    @TableField("file_type")
    private String fileType;

    @ApiModelProperty("附件路径")
    @TableField("file_path")
    @NormalizeFilePath
    private String filePath;

    @ApiModelProperty("文章id")
    @TableField("article_id")
    private Integer articleId;
}
