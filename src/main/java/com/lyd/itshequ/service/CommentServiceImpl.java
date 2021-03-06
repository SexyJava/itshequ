package com.lyd.itshequ.service;

import com.lyd.itshequ.bean.CommentDTO;
import com.lyd.itshequ.enums.CommentTypeEnum;
import com.lyd.itshequ.enums.NotificationEnum;
import com.lyd.itshequ.enums.NotificationStatusEnum;
import com.lyd.itshequ.exception.MeErrorCode;
import com.lyd.itshequ.exception.MeExceptions;
import com.lyd.itshequ.mapper.CommentMapper;
import com.lyd.itshequ.mapper.NotificationMapper;
import com.lyd.itshequ.mapper.PostMapper;
import com.lyd.itshequ.mapper.UserMapper;
import com.lyd.itshequ.model.Comment;
import com.lyd.itshequ.model.Notification;
import com.lyd.itshequ.model.Post;
import com.lyd.itshequ.model.User;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService{

    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private PostMapper postMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private NotificationMapper notificationMapper;

    @Override
    @Transactional
    //抛出异常数据回滚
    public void insert(Comment comment, User user) {
        if (comment.getParentId() == null || comment.getParentId()==0){
            throw new MeExceptions(MeErrorCode.TARGET_PARAM_NOT_FOUNT);
        }
        if(comment.getType() == null || !CommentTypeEnum.isExist(comment.getType())){
            throw new MeExceptions(MeErrorCode.TYPE_PARAM_WRONG);
        }
        if (comment.getType().equals(CommentTypeEnum.COMMENT.getType())){
            //回复评论
            Comment dbComment = commentMapper.getByCommentId(comment.getId());
            if (dbComment==null){
                throw new MeExceptions(MeErrorCode.COMMENT_NOT_FOUND);
            }
            Post postById = postMapper.getPostById(dbComment.getParentId());
            if (postById == null){
                throw new MeExceptions(MeErrorCode.POST_NOT_FOUND);
            }
            commentMapper.insert(comment);
            comment.setCommentCount(dbComment.getCommentCount()+1);
            commentMapper.incCommentCount(comment);
            //创建通知
            createNotify(comment, dbComment.getCommentator(), NotificationEnum.REPLY_COMMENT, user.getName(), postById.getTitle(), postById.getId());
        }else{
            //回复问题
            Post postById = postMapper.getPostById(comment.getParentId());
            if (postById == null){
                throw new MeExceptions(MeErrorCode.POST_NOT_FOUND);
            }
            comment.setCommentCount(0);
            commentMapper.insert(comment);
            postById.setCommentCount(postById.getCommentCount()+1);
            postMapper.updatePost(postById);
            //创建通知
            createNotify(comment, postById.getCreator(), NotificationEnum.REPLY_POST,user.getName(),postById.getTitle(), postById.getId());
        }
    }

    /**
     * 创建通知
     * @param comment
     * @param receiver
     * @param notificationEnum
     * @param name
     * @param postTitle
     * @param outerId
     */
    private void createNotify(Comment comment, Long receiver, NotificationEnum notificationEnum, String name, String postTitle, Long outerId) {
        if(receiver == comment.getCommentator()){
            return;
        }
        Notification notification = new Notification();
        notification.setGmtCreate(System.currentTimeMillis());
        notification.setType(notificationEnum.getType());
        notification.setOuterId(outerId);
        notification.setNotifier(comment.getCommentator());
        notification.setStatus(NotificationStatusEnum.UNREAD.getStatus());
        notification.setReceiver(receiver);
        notification.setNotifierName(name);
        notification.setOuterTitle(postTitle);
        notificationMapper.insert(notification);
    }

    @Override
    public List<CommentDTO> listByTargetId(Long id, CommentTypeEnum type) {

        List<Comment> comments=commentMapper.selectCommentByIdAndType(id, type.getType());
        if (comments.size() == 0){
            return new ArrayList<>();
        }
        //使用lamda获取去重的评论人
        Set<Long> commentators = comments.stream().map(comment -> comment.getCommentator()).collect(Collectors.toSet());
        List<Long> userIds = new ArrayList<>();
        userIds.addAll(commentators);
        //获取评论人并转换为map
        List<User> users = userMapper.selectByIdList(userIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(user -> user.getId(), user -> user));
        //转换comment为commentDTO
        List<CommentDTO> commentDTOS = comments.stream().map(comment -> {
            CommentDTO commentDTO = new CommentDTO();
            BeanUtils.copyProperties(comment,commentDTO);
            commentDTO.setUser(userMap.get(comment.getCommentator()));
            return commentDTO;
        }).collect(Collectors.toList());

        return commentDTOS;
    }
}
