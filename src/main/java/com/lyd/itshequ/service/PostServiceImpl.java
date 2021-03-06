package com.lyd.itshequ.service;

import com.lyd.itshequ.bean.PageDTO;
import com.lyd.itshequ.bean.PostDTO;
import com.lyd.itshequ.exception.MeErrorCode;
import com.lyd.itshequ.exception.MeExceptions;
import com.lyd.itshequ.mapper.PostMapper;
import com.lyd.itshequ.mapper.UserMapper;
import com.lyd.itshequ.model.Post;
import com.lyd.itshequ.model.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName PostServiceImpl
 * @Description TODO
 * @Author Liuyunda
 * @Date 2020/2/23 12:10
 **/
@Service
public class PostServiceImpl implements PostService {
	@Autowired
	private PostMapper postMapper;
	@Autowired
	private UserMapper userMapper;

	@Override
	public PageDTO getPostByCreator(Long id, Integer page, Integer pageSize) {
		Integer pageSum= postMapper.pageSumById(id);
		if (page<1){
			page = 1;
		}

		if (page>pageSum/pageSize+1){
			page = pageSum/pageSize+1;
		}
		Integer offSize = pageSize * (page-1);
		List<Post> postAll = postMapper.getPostByCreator(id,offSize,pageSize);
		List<PostDTO> postDTOS = new ArrayList<>();
		PageDTO pageDTO = new PageDTO();
		for (Post post : postAll){
			User user = userMapper.findById(post.getCreator());
			PostDTO postDTO = new PostDTO();
			BeanUtils.copyProperties(post,postDTO);
			postDTO.setUser(user);
			postDTOS.add(postDTO);
		}
		pageDTO.setData(postDTOS);

		pageDTO.setPageInfo(pageSum,page,pageSize);
		return pageDTO;
	}


	@Override
	public PageDTO getPostAll(String search, Integer page, Integer pageSize) {
		Integer pageSum;
		if (StringUtils.isNotBlank(search)){
			pageSum=postMapper.pageSumBySearch(search);
		}else {
			pageSum = postMapper.pageSum();
		}
		if (page<1){
			page = 1;
		}

		if (page>pageSum/pageSize+1){
			page = pageSum/pageSize+1;
		}
		Integer offSize = pageSize * (page-1);
		List<Post> postAll;
		PageDTO pageDTO = new PageDTO();
		if (StringUtils.isNotBlank(search)){
			postAll=postMapper.getPostBySearch(search,offSize,pageSize);
			pageDTO.setIsPage(false);
			pageDTO.setSearch(search);
		}else{
			postAll= postMapper.getPostAll(offSize,pageSize);
		}
		List<PostDTO> postDTOS = new ArrayList<>();
		for (Post post : postAll){
			User user = userMapper.findById(post.getCreator());
			PostDTO postDTO = new PostDTO();
			BeanUtils.copyProperties(post,postDTO);
			postDTO.setUser(user);
			postDTOS.add(postDTO);
		}
		pageDTO.setData(postDTOS);
		pageDTO.setPageInfo(pageSum,page,pageSize);
		return pageDTO;
	}

	@Override
	public PostDTO getPostById(Long id) {
		Post post = postMapper.getPostById(id);
		if (post == null){
			throw new MeExceptions(MeErrorCode.POST_NOT_FOUND);
		}
		PostDTO postDTO = new PostDTO();
		BeanUtils.copyProperties(post,postDTO);
		User user = userMapper.findById(post.getCreator());
		postDTO.setUser(user);
		return postDTO;
	}

	@Override
	public void createOrUpdate(Post post) {
		Post postById = postMapper.getPostById(post.getId());
		if (postById!=null){
			post.setGmtCreate(System.currentTimeMillis());
			post.setWatchCount(postById.getWatchCount());
			post.setCommentCount(postById.getCommentCount());
			int i = postMapper.updatePost(post);
			if (i != 1){
				throw new MeExceptions(MeErrorCode.POST_NOT_FOUND);
			}
		}else {
			post.setGmtCreate(System.currentTimeMillis());
			post.setGmtModified(System.currentTimeMillis());
			post.setWatchCount(0);
			post.setCommentCount(0);
			post.setLikeCount(0);
			postMapper.create(post);
		}
	}

	@Override
	public PostDTO incView(Long id) {
		Post postById = postMapper.getPostById(id);
		if (postById == null){
			throw new MeExceptions(MeErrorCode.POST_NOT_FOUND);
		}
		postById.setWatchCount(postById.getWatchCount()+1);
		int i = postMapper.updatePost(postById);
		PostDTO postDTO = new PostDTO();
		User user = userMapper.findById(postById.getCreator());
		postDTO.setUser(user);
		BeanUtils.copyProperties(postById,postDTO);
		return postDTO;
	}

	@Override
	public List<PostDTO> selectByRelated(PostDTO postById) {
		if (StringUtils.isBlank(postById.getTag())){
			return new ArrayList<>();
		}
		String newTag = StringUtils.replace(postById.getTag(), ",", "|");
		Post post = new Post();
		post.setId(postById.getId());
		post.setTag(newTag);
		List<Post> posts = postMapper.queryByTag(post);
		List<PostDTO> postDTOS = posts.stream().map(p -> {
			PostDTO postDTO = new PostDTO();
			BeanUtils.copyProperties(p,postDTO);
			return postDTO;
		}).collect(Collectors.toList());
		return postDTOS;
	}

	@Override
	public List<Post> UPLeaderBoard() {
		List<Post> posts = postMapper.UPLeaderBoard();
		return posts;
	}
}
