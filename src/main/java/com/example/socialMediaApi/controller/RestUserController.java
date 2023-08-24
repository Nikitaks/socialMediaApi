package com.example.socialMediaApi.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.socialMediaApi.config.JwtTokenUtil;
import com.example.socialMediaApi.entity.FriendsSubscribers;
import com.example.socialMediaApi.entity.JwtRequest;
import com.example.socialMediaApi.entity.JwtResponse;
import com.example.socialMediaApi.entity.Message;
import com.example.socialMediaApi.entity.Post;
import com.example.socialMediaApi.entity.Response;
import com.example.socialMediaApi.entity.User;
import com.example.socialMediaApi.repo.FriendsSubscribersRepository;
import com.example.socialMediaApi.repo.MessageRepository;
import com.example.socialMediaApi.repo.PostRepository;
import com.example.socialMediaApi.repo.UserRepository;
import com.example.socialMediaApi.security.MySQLUserDetailsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "API Controller", description = "Social media API controller, which realizes full functional")
@RestController
public class RestUserController {
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private PostRepository postRepository;
	@Autowired
	private  FriendsSubscribersRepository friendsSubscribersRepository;
	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private AuthenticationManager authenticationManager;
	@Autowired
	private JwtTokenUtil jwtTokenUtil;
	@Autowired
	private MySQLUserDetailsService userDetailsService;

	private Logger logger = Logger.getLogger("RestUserController");

	@Operation(summary = "Unsecure. Checks if user loggined or returns message with register instructions")
    @ApiResponses(value = {
    	@ApiResponse(
        	responseCode = "200",
        	description = "Message with name of logined user or with register instructions, if user unloggined or jwt-token is expired",
        	content = {
            	@Content(
                	mediaType = "application/json",
                	array = @ArraySchema(schema = @Schema(implementation = Response.class)))
            })
    })
	@GetMapping("/account")
	public Response ifRegisterUser(Authentication authentication) {
		if (authentication != null) {
			return new Response("You are registered. Your name: "
				+ authentication.getName());
		}
		else
			return new Response("You aren't registered. To register user POST to account/new "
				+ "message with JSON contain name, password and email fields");
	}

	@Operation(summary = "Unsecure. Registers new user")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "Id field is ignored and may be absent")
	@ApiResponses(value = {
    	@ApiResponse(
        	responseCode = "201",
        	description = "User registered successfully",
        	content = {
            	@Content(
                	mediaType = "application/json",
                	array = @ArraySchema(schema = @Schema(implementation = Response.class)))
            }),
    	@ApiResponse(
           	responseCode = "400",
           	description = "One of messages: user alredy exists; "
           			+ "name, password and email couldn't be empty or absent",
           	content = {
               	@Content(
                   	mediaType = "application/json",
                   	array = @ArraySchema(schema = @Schema(implementation = Response.class)))
           		})
    })
	@PostMapping("/account/new")
	public ResponseEntity<Response> registerUser(@RequestBody User user, Authentication authentication) {
		if (authentication != null) {
			return ResponseEntity.badRequest()
				.body(new Response("You are registered alredy. Your name: "
				+ authentication.getName()));
		}
		List<User> checkIfExistsList =  userRepository.findByName(user.getName());
		if (checkIfNamePasswordEmailEmptyOrNull(user)) {
			return ResponseEntity.badRequest().body(new Response("Name, password and email couldn't be empty or absent"));
		}
		if (checkIfExistsList.isEmpty()) {
			String encodedPassword = passwordEncoder.encode(user.getPassword());
			user.setPassword(encodedPassword);
			User savedUser = userRepository.save(user);
			if (savedUser.isTheSameUser(user)) {
				return ResponseEntity.created(null)
					.body(new Response("User created"));
			}
			else {
				return ResponseEntity.badRequest()
					.body(new Response("Database saving problem"));
			}
		}
		else {
			return ResponseEntity.badRequest()
				.body(new Response("User alredy exists"));
		}
	}

	@Operation(summary = "Unsecure. Logging in service. It returns JWT-token, which used for authentication "
			+ "for all secure endpoints. Client sends JWT in Authentication "
			+ "header with Bearer format. In Swagger-UI it may to attach "
			+ "JWT-token in each request, by entering it into Authorize-button.")
    @ApiResponses(value = {
    	@ApiResponse(
        	responseCode = "200",
        	description = "Message with JWT token, if username exists and password correct",
        	content = {
            	@Content(
                	mediaType = "application/json",
                	array = @ArraySchema(schema = @Schema(implementation = JwtResponse.class))),

            }),
    	@ApiResponse(
           	responseCode = "401",
           	description = "Message with bad credentials information, if username or password incorrect",
           	content = {
               	@Content(
                   	mediaType = "application/json",
                   	array = @ArraySchema(schema = @Schema(implementation = Response.class))),
            })
    })
	@PostMapping("/account/login")
	public ResponseEntity<?> login(@RequestBody JwtRequest authenticationRequest) {
		try {
			authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());
		}
		catch (Exception e) {
			return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(new Response("Username or password incorrect"));
		}
		final UserDetails userDetails = userDetailsService
				.loadUserByUsername(authenticationRequest.getUsername());

		final String token = jwtTokenUtil.generateToken(userDetails);

		return ResponseEntity.ok(new JwtResponse(token));
	}

	private void authenticate(String username, String password) throws Exception {
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
		} catch (DisabledException e) {
			throw new Exception("USER_DISABLED", e);
		} catch (BadCredentialsException e) {
			throw new Exception("INVALID_CREDENTIALS", e);
		}
	}

	@Operation(summary = "Posting new public messages")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "To post the message, id, user and dateAndTime fields is ignored and may be absent")
	@ApiResponses(value = {
    	@ApiResponse(
        	responseCode = "201",
        	description = "Message created succeccfully",
        	content = {
            	@Content(
                	mediaType = "application/json",
                	array = @ArraySchema(schema = @Schema(implementation = Response.class))),
            }),
    	@ApiResponse(
           	responseCode = "400",
           	description = "User not exists",
           	content = {
               	@Content(
                   	mediaType = "application/json",
                   	array = @ArraySchema(schema = @Schema(implementation = Response.class))),
            })
    })
	@PostMapping("/post/new")
	public ResponseEntity<Response> newPost(@RequestBody Post post, Authentication authentication) {

		User user = userRepository.findFirstUser_idByName(authentication.getName());
		if (user != null) {
			post.setUser(user.getUser_id());
			post.setDateAndTime(LocalDateTime.now());
			Post savedPost = postRepository.save(post);
			if (savedPost.isTheSamePost(post)) {
				return ResponseEntity.created(null)
						.body(new Response("Post created"));
			}
			else {
				return ResponseEntity.badRequest()
					.body(new Response("Database saving problem"));
			}			
		}
		else {
			return ResponseEntity
				.badRequest()
				.body(new Response("User not exists"));
		}
	}

	@Operation(summary = "Shows posted messages of user with path userId")
    @ApiResponses(value = {
    	@ApiResponse(
        	responseCode = "200",
        	description = "List of posted messages",
        	content = {
            	@Content(
                	mediaType = "application/json",
                	array = @ArraySchema(schema = @Schema(implementation = Response.class))),
            }),
    	@ApiResponse(
           	responseCode = "400",
           	description = "User not exists",
           	content = {
               	@Content(
                   	mediaType = "application/json",
                   	array = @ArraySchema(schema = @Schema(implementation = Response.class))),
            })
    })
	@GetMapping("/post/show/{userId}")
	public Object showPost(@PathVariable Long userId) {
		Optional<User> optionalUser = userRepository.findById(userId);
		if (! optionalUser.isPresent()) {
			return ResponseEntity
				.badRequest()
				.body(new Response("User not exists"));
		}
		return postRepository.findByUser(userId);
	}

	@Operation(summary = "Deletes posted messages with path id")
    @ApiResponses(value = {
    	@ApiResponse(
        	responseCode = "200",
        	description = "Post deleted",
        	content = {
            	@Content(
                	mediaType = "application/json",
                	array = @ArraySchema(schema = @Schema(implementation = Response.class))),
            }),
    	@ApiResponse(
           	responseCode = "400",
           	description = "Post belongs another user, or not exists",
           	content = {
               	@Content(
                   	mediaType = "application/json",
                   	array = @ArraySchema(schema = @Schema(implementation = Response.class))),
               })
    })
	@DeleteMapping("/post/delete/{id}")
	public ResponseEntity<Response> deletePost(@PathVariable Long id, Authentication authentication) {
		Optional<Post> optionalPost = postRepository.findById(id);
		if (! optionalPost.isPresent()) {
			return ResponseEntity
				.badRequest()
				.body(new Response("Post not exists"));
		}
		User user = userRepository.findFirstUser_idByName(authentication.getName());
		if (optionalPost.get().getUser() == user.getUser_id()) {
			postRepository.deleteById(id);
			return ResponseEntity.ok()
				.body(new Response("Post deleted"));
		}
		else {
			return ResponseEntity
				.badRequest()
				.body(new Response("Post belongs another user"));
		}
	}

	@Operation(summary = "Updates posted messages")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "To update the post, user and dateAndTime fields is ignored and may be absent")
	@ApiResponses(value = {
    	@ApiResponse(
        	responseCode = "200",
        	description = "Post updated",
        	content = {
            	@Content(
                	mediaType = "application/json",
                	array = @ArraySchema(schema = @Schema(implementation = Response.class))),
            }),
    	@ApiResponse(
           	responseCode = "400",
           	description = "Post belongs another user, or not exists",
           	content = {
               	@Content(
                   	mediaType = "application/json",
                   	array = @ArraySchema(schema = @Schema(implementation = Response.class))),
               })
    })
	@PatchMapping("/post/update")
	public ResponseEntity<Response> updatePost(@RequestBody Post post, Authentication authentication) {
		Optional<Post> optionalPost = postRepository.findById(post.getId());
		if (! optionalPost.isPresent()) {
			return ResponseEntity
				.badRequest()
				.body(new Response("Post not exists"));
		}
		User user = userRepository.findFirstUser_idByName(authentication.getName());
		if (optionalPost.get().getUser() != user.getUser_id()) {
			return ResponseEntity
				.badRequest()
				.body(new Response("Post belongs another user"));
		}
		postRepository.save(post);
		return ResponseEntity.ok()
			.body(new Response("Post updated"));
	}

	@Operation(summary = "Subscribes on user with path userId")
    @ApiResponses(value = {
    	@ApiResponse(
        	responseCode = "200",
        	description = "One of messages: subscribed sucsessfully; you are friends alredy; you are subscriber alredy; friendship request passed",
        	content = {
            	@Content(
                	mediaType = "application/json",
                	array = @ArraySchema(schema = @Schema(implementation = Response.class))),
            }),
    	@ApiResponse(
           	responseCode = "400",
           	description = "One of messages: user not exists, to subsctibe self is forbidden",
           	content = {
               	@Content(
                   	mediaType = "application/json",
                   	array = @ArraySchema(schema = @Schema(implementation = Response.class))),
            }),
    	@ApiResponse(
    		responseCode = "500",
        	description = "Internal server error: broken entryes in subscribe database",
        	content = {
        		@Content(
        		mediaType = "application/json",
        		array = @ArraySchema(schema = @Schema(implementation = Response.class))),
    		})
    })
	@PostMapping("/friend/subscribe/{userId}")
	public ResponseEntity<Response> subscribe(@PathVariable Long userId, Authentication authentication) {

		Optional<User> optionalUser = userRepository.findById(userId);
		if (! optionalUser.isPresent()) {
			return ResponseEntity
				.badRequest()
				.body(new Response("User not exists"));
		}
		User subscriber = userRepository.findFirstUser_idByName(authentication.getName());
		if (subscriber.getUser_id() == userId) {
			return ResponseEntity
				.badRequest()
				.body(new Response("To subsctibe self is forbidden"));
		}

		return subscribeToUser(subscriber.getUser_id(), userId);
	}

	@Operation(summary = "Unsubscribes user with path userId")
    @ApiResponses(value = {
    	@ApiResponse(
        	responseCode = "200",
        	description = "One of messages: you are not subscribed; unsubscribed",
        	content = {
            	@Content(
                	mediaType = "application/json",
                	array = @ArraySchema(schema = @Schema(implementation = Response.class))),
            }),
    	@ApiResponse(
           	responseCode = "400",
           	description = "User not exists",
           	content = {
               	@Content(
                   	mediaType = "application/json",
                   	array = @ArraySchema(schema = @Schema(implementation = Response.class))),
            }),
    	@ApiResponse(
           	responseCode = "500",
           	description = "Internal server error: broken entryes in subscribe database",
           	content = {
               	@Content(
                   	mediaType = "application/json",
                   	array = @ArraySchema(schema = @Schema(implementation = Response.class))),
            })
    })
	@PostMapping("/friend/unsubscribe/{userId}")
	public ResponseEntity<Response> unsubscribe(@PathVariable Long userId, Authentication authentication) {

		Optional<User> optionalUser = userRepository.findById(userId);
		if (! optionalUser.isPresent()) {
			return ResponseEntity
				.badRequest()
				.body(new Response("User not exists"));
		}
		User subscriber = userRepository.findFirstUser_idByName(authentication.getName());

		return unsubscribeUser(subscriber.getUser_id(), userId);
	}

	@Operation(summary = "Sends message to user with path userId")
    @ApiResponses(value = {
    	@ApiResponse(
        	responseCode = "200",
        	description = "Message sent",
        	content = {
            	@Content(
                	mediaType = "application/json",
                	array = @ArraySchema(schema = @Schema(implementation = Response.class))),
            }),
    	@ApiResponse(
           	responseCode = "400",
           	description = "One message of: user not exists; to send message self is forbidden, users are not friends",
           	content = {
               	@Content(
                   	mediaType = "application/json",
                   	array = @ArraySchema(schema = @Schema(implementation = Response.class))),
            })
    })
	@PostMapping("/message/send/{userId}")
	public ResponseEntity<Response> sendMessage(@PathVariable Long userId, @RequestBody Response data, Authentication authentication) {
		Optional<User> optionalUser = userRepository.findById(userId);
		if (! optionalUser.isPresent()) {
			return ResponseEntity
				.badRequest()
				.body(new Response("User not exists"));
		}
		Long fromUser = userRepository.findFirstUser_idByName(authentication.getName()).getUser_id();
		if (fromUser == userId) {
			return ResponseEntity
				.badRequest()
				.body(new Response("To send message self is forbidden"));
		}
		if (! isFriends(fromUser, userId)) {
			return ResponseEntity
				.badRequest()
				.body(new Response("Users are not friends"));
		}
		Message message = new Message(0, fromUser, userId, LocalDateTime.now(), data.getMessage());
		messageRepository.save(message);
		return ResponseEntity
				.ok()
				.body(new Response("Message sent"));
	}

	@Operation(summary = "Shows message to user with path userId")
    @ApiResponses(value = {
    	@ApiResponse(
        	responseCode = "200",
        	description = "Messages list",
        	content = {
            	@Content(
                	mediaType = "application/json",
                	array = @ArraySchema(schema = @Schema(implementation = Message.class))),
            }),
    	@ApiResponse(
           	responseCode = "400",
           	description = "User not exists",
           	content = {
               	@Content(
                   	mediaType = "application/json",
                   	array = @ArraySchema(schema = @Schema(implementation = Response.class))),
            })
    })
	@GetMapping("/message/show/{userId}")
	public Object getMessages(@PathVariable Long userId, Authentication authentication) {
		Optional<User> optionalUser = userRepository.findById(userId);
		if (! optionalUser.isPresent()) {
			return ResponseEntity
				.badRequest()
				.body(new Response("User not exists"));
		}
		Long fromUser = userRepository.findFirstUser_idByName(authentication.getName()).getUser_id();

		return messageRepository.findMessages(fromUser, userId);
	}

	@Operation(summary = "Shows posts of users, which subscribed on")
	@Parameters(
		@Parameter(name = "pageable",
			description = "To form post list it need set page and sort parameters. "
			+ "Page-parameter minimal value is 0, size-parameter means "
			+ "number of posts on page. Sort parameter is an array, which "
			+ "includes sorted fiels and sorting direction. For example,"
			+ "to sort by id ascending and date in descending it need to set like in example."
			+ "Directions asc or desc can be absent, asc by default.",
			example = "{\r\n" +
					"  \"page\": 0,\r\n" +
					"  \"size\": 10,\r\n" +
					"  \"sort\": [\r\n" +
					"     \"user,asc\", \"dateAndTime,desc\"\r\n" +
					"  ]\r\n" +
					"}")
			)
	@ApiResponses(value = {
    	@ApiResponse(
        	responseCode = "200",
        	description = "Posts list",
        	content = {
            	@Content(
                	mediaType = "application/json",
                	array = @ArraySchema(schema = @Schema(implementation = Post.class))),
            })
    })
	@GetMapping("/post/newsfeed")
	public List<Post> newsfeed(Authentication authentication, Pageable pageable) {
		List<Long> authorsList = authors(authentication);
		List<Post> postList = postRepository.findByUserIn(authorsList, pageable);
		return postList;
	}

	private boolean checkIfNamePasswordEmailEmptyOrNull(User user) {
		return "".equals(user.getEmail()) || "".equals(user.getName())
			|| "".equals(user.getPassword())
			|| user.getEmail() == null || user.getName() == null
			|| user.getPassword() == null;
	}

	private List<Long> authors(Authentication authentication) {
		Long user = userRepository.findFirstUser_idByName(authentication.getName()).getUser_id();

		List<FriendsSubscribers> list = friendsSubscribersRepository
			.findByUser1AndStatus(user, FriendsSubscribers.Status.UsersFrends);
		list.addAll(friendsSubscribersRepository
			.findByUser1AndStatus(user, FriendsSubscribers.Status.User1subscribedUser2));
		list.addAll(friendsSubscribersRepository
			.findByUser2AndStatus(user, FriendsSubscribers.Status.UsersFrends));
		list.addAll(friendsSubscribersRepository
			.findByUser2AndStatus(user, FriendsSubscribers.Status.User1subscribedUser2));
		List<Long> authorsList = list.stream()
			.map(item -> item.getUser1() == user ? item.getUser2() : item.getUser1())
			.collect(Collectors.toList());

		return authorsList;
	}

	@ExceptionHandler({Exception.class})
    public ResponseEntity<Response> handleException(Exception e, HttpServletRequest request) {
		return ResponseEntity
			.internalServerError()
			.body(new Response("Internal server exception. Message: "
					+ e.getMessage() + " Path: "
					+ request.getServletPath().toString()));
    }

	private boolean isFriends(Long fromUser, Long userId) {
		List<FriendsSubscribers> list = friendsSubscribersRepository.findByUser1AndUser2(fromUser, userId);
		list.addAll(friendsSubscribersRepository.findByUser1AndUser2(userId, fromUser));
		if (list.size() > 1) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Broken entryes in subscribe database");
		}
		if (list.isEmpty()) {
			return false;
		}
		FriendsSubscribers subscription = list.get(0);
		return subscription.getStatus() == FriendsSubscribers.Status.UsersFrends;
	}

	private ResponseEntity<Response> unsubscribeUser(Long subscriber, Long userId) {
		if ((subscriber == null) || (userId == null)) {
			return ResponseEntity
				.badRequest()
				.body(new Response("No users id's"));
		}
		List<FriendsSubscribers> list = friendsSubscribersRepository.findByUser1AndUser2(subscriber, userId);
		list.addAll(friendsSubscribersRepository.findByUser1AndUser2(userId, subscriber));
		if (list.size() > 1) {
			return ResponseEntity
				.internalServerError()
				.body(new Response("Broken entryes in subscribe database"));
		}
		if (list.isEmpty()) {
			return ResponseEntity
				.ok()
				.body(new Response("You are not subscribed"));
		}

		ResponseEntity<Response> result = null;
		FriendsSubscribers subscription = list.get(0);
		switch (subscription.getStatus()) {
		case UsersFrends:
			if (subscription.getUser1() == subscriber) {
				subscription.setStatus(FriendsSubscribers.Status.User2subscribedUser1);
			}
			else {
				subscription.setStatus(FriendsSubscribers.Status.User1subscribedUser2);
			}
			friendsSubscribersRepository.save(subscription);
			result = ResponseEntity
				.ok()
				.body(new Response("Unsubscribed"));
			break;
		case User1subscribedUser2:
			if (subscription.getUser1() == subscriber) {
				friendsSubscribersRepository.delete(subscription);
				result = ResponseEntity
					.ok()
					.body(new Response("Unsubscribed"));
			}
			else {
				result = ResponseEntity
					.ok()
					.body(new Response("You are not subscribed"));
			}
			break;
		case User2subscribedUser1:
			if (subscription.getUser2() == subscriber) {
				friendsSubscribersRepository.delete(subscription);
				result = ResponseEntity
					.ok()
					.body(new Response("Unsubscribed"));
			}
			else {
				result = ResponseEntity
					.ok()
					.body(new Response("You are not subscribed"));
			}
		}
		return result;
	}

	private ResponseEntity<Response> subscribeToUser(Long subscriber, Long userId) {
		if ((subscriber == null) || (userId == null)) {
			return ResponseEntity
				.badRequest()
				.body(new Response("No users id's"));
		}

		List<FriendsSubscribers> list = friendsSubscribersRepository.findByUser1AndUser2(subscriber, userId);
		list.addAll(friendsSubscribersRepository.findByUser1AndUser2(userId, subscriber));

		if (list.size() > 1) {
			return ResponseEntity
				.internalServerError()
				.body(new Response("Broken entryes in subscribe database"));
		}
		if (list.isEmpty()) {
			FriendsSubscribers subscription = new FriendsSubscribers(0, subscriber, userId, FriendsSubscribers.Status.User1subscribedUser2);
			friendsSubscribersRepository.save(subscription);
			return ResponseEntity
				.ok()
				.body(new Response("Subscribed"));
		}
		ResponseEntity<Response> result = null;
		FriendsSubscribers subscription = list.get(0);
		switch (subscription.getStatus()) {
		case UsersFrends:
			result = ResponseEntity
				.ok()
				.body(new Response("You are friends alredy"));
			break;
		case User1subscribedUser2:
			if (subscription.getUser1() == subscriber) {
				result = ResponseEntity
					.ok()
					.body(new Response("You are subscriber alredy"));
			}
			else {
				subscription.setStatus(FriendsSubscribers.Status.UsersFrends);
				friendsSubscribersRepository.save(subscription);
				result = ResponseEntity
					.ok()
					.body(new Response("Friendship request passed"));
			}
			break;
		case User2subscribedUser1:
			if (subscription.getUser2() == subscriber) {
				result = ResponseEntity
					.ok()
					.body(new Response("You are subscriber alredy"));
			}
			else {
				subscription.setStatus(FriendsSubscribers.Status.UsersFrends);
				friendsSubscribersRepository.save(subscription);
				result = ResponseEntity
					.ok()
					.body(new Response("Friendship request passed"));
			}
		}
		return result;
	}

	public PasswordEncoder getPasswordEncoder() {
		return passwordEncoder;
	}

	public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public PostRepository getPostRepository() {
		return postRepository;
	}

	public void setPostRepository(PostRepository postRepository) {
		this.postRepository = postRepository;
	}

	public FriendsSubscribersRepository getFriendsSubscribersRepository() {
		return friendsSubscribersRepository;
	}

	public void setFriendsSubscribersRepository(FriendsSubscribersRepository friendsSubscribersRepository) {
		this.friendsSubscribersRepository = friendsSubscribersRepository;
	}

	public MessageRepository getMessageRepository() {
		return messageRepository;
	}

	public void setMessageRepository(MessageRepository messageRepository) {
		this.messageRepository = messageRepository;
	}

	public AuthenticationManager getAuthenticationManager() {
		return authenticationManager;
	}

	public void setAuthenticationManager(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	public JwtTokenUtil getJwtTokenUtil() {
		return jwtTokenUtil;
	}

	public void setJwtTokenUtil(JwtTokenUtil jwtTokenUtil) {
		this.jwtTokenUtil = jwtTokenUtil;
	}

	public MySQLUserDetailsService getUserDetailsService() {
		return userDetailsService;
	}

	public void setUserDetailsService(MySQLUserDetailsService userDetailsService) {
		this.userDetailsService = userDetailsService;
	}

	public Logger getLogger() {
		return logger;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}
}
