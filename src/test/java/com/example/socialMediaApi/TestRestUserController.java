package com.example.socialMediaApi;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.socialMediaApi.config.JwtTokenUtil;
import com.example.socialMediaApi.controller.RestUserController;
import com.example.socialMediaApi.entity.JwtRequest;
import com.example.socialMediaApi.entity.JwtResponse;
import com.example.socialMediaApi.entity.Response;
import com.example.socialMediaApi.entity.User;
import com.example.socialMediaApi.repo.UserRepository;
import com.example.socialMediaApi.security.MySQLUserDetailsService;


@ExtendWith(MockitoExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class TestRestUserController {

	private final String userName = "AnyUserName";
	private final String password = "AnyPassword";
	private final String email = "email@domain.com";
	private final String encodedPassword = "AnyEncodedPassword";
	private final String testToken = "header.payload.signature";
	private final RestUserController controller = new RestUserController();
	private final User user = new User(userName, password, email);

	private Authentication authenticationWithName;
	private UserRepository userRepository;
	private PasswordEncoder passwordEncoder;
	private AuthenticationManager authenticationManager;
	private MySQLUserDetailsService userDetailsService;
	private JwtTokenUtil jwtTokenUtil;

	@BeforeAll
	public void beforeEach()  {
		authenticationWithName = Mockito.mock(Authentication.class);
		Mockito.when(authenticationWithName.getName()).thenReturn(userName);
		userRepository = Mockito.mock(UserRepository.class);
		controller.setUserRepository(userRepository);
		passwordEncoder = Mockito.mock(PasswordEncoder.class);
		controller.setPasswordEncoder(passwordEncoder);
		Mockito.when(passwordEncoder.encode(user.getPassword())).thenReturn(encodedPassword);
		authenticationManager = Mockito.mock(AuthenticationManager.class);
		userDetailsService = Mockito.mock(MySQLUserDetailsService.class);
		controller.setAuthenticationManager(authenticationManager);
		controller.setUserDetailsService(userDetailsService);
		jwtTokenUtil = Mockito.mock(JwtTokenUtil.class);
		controller.setJwtTokenUtil(jwtTokenUtil);
	}

	@Test
	public void ifRegisterUser() {
		//arrange
		Authentication authentication = null;
		//act
		Response response = controller.ifRegisterUser(authentication);
		//assert
		Assertions.assertEquals(new Response("You aren't registered. To register user POST to account/new "
			+ "message with JSON contain name, password and email fields"), response,
				"Unexpected unathorize behavior");

		response = controller.ifRegisterUser(authenticationWithName);
		Assertions.assertEquals(new Response("You are registered. Your name: "
			+ userName), response,
			"Unexpected athorize behavior");
	}

	@Test
	public void registerUser() {
		//arrange
		//act
		ResponseEntity<Response> response = controller.registerUser(user, authenticationWithName);
		//assert
		Assertions.assertEquals(ResponseEntity.badRequest()
			.body(new Response("You are registered alredy. Your name: "
			+ userName)), response,
			"Unexpected athorize behavior");

		List<User> list = new ArrayList<User>();
		list.add(new User());
		Mockito.doReturn(list).when(userRepository).findByName(Mockito.anyString());
		response = controller.registerUser(user, null);
		Assertions.assertEquals(ResponseEntity.badRequest()
			.body(new Response("User alredy exists")), response,
			"Unexpected user exists behavior");

		Mockito.doReturn(new ArrayList<User>()).when(userRepository).findByName(Mockito.anyString());
		User userClone = new User(user.getName(), user.getPassword(), user.getEmail());
		User userWithEncodedPassword = new User(user.getName(), encodedPassword, user.getEmail());
		User userReturnedDatabase = new User(user.getName(), encodedPassword, user.getEmail());
		Mockito.doReturn(userReturnedDatabase).when(userRepository).save(userWithEncodedPassword);
		response = controller.registerUser(userClone, null);
		Assertions.assertEquals(ResponseEntity.created(null)
				.body(new Response("User created")), response,
			"Unexpected user creation behavior");

		userClone = new User(user.getName(), user.getPassword(), user.getEmail());
		userReturnedDatabase.setPassword(userReturnedDatabase.getPassword()+"*");
		response = controller.registerUser(userClone, null);
		Assertions.assertEquals(ResponseEntity.badRequest()
				.body(new Response("Database saving problem")), response,
			"Unexpected database saving problem behavior");
		
		String[][] userBadCredentialsMatrix = {
				{null, 				user.getPassword(), user.getEmail()},
				{user.getName(), 	null, 				user.getEmail()},
				{user.getName(), 	user.getPassword(), null},
				{null, 				null, 				user.getEmail()},
				{null, 				user.getPassword(), null},
				{user.getName(), 	null, 				null},
				{null,				null,				null},
				{"", 				user.getPassword(), user.getEmail()},
				{user.getName(), 	"", 				user.getEmail()},
				{user.getName(), 	user.getPassword(), ""},
				{"", 				"", 				user.getEmail()},
				{"", 				user.getPassword(), ""},
				{user.getName(), 	"", 				""},
				{"",				"",					""}		
			};
		for (String[] userBadCredentialsArray :  userBadCredentialsMatrix) {
			userClone = new User(userBadCredentialsArray [0], 
								 userBadCredentialsArray[1],
								 userBadCredentialsArray[2]);
			response = controller.registerUser(userClone, null);
			Assertions.assertEquals(ResponseEntity.badRequest()
				.body(new Response("Name, password and email couldn't "
				+ "be empty or absent")), response,
				"Unexpected null or empty string credentials behavior");
		}
	}
	

	@Test
	public void loginAndAuthenticate() {
		JwtRequest authenticationRequest = new JwtRequest(userName, password);
		UserDetails userDetails = Mockito.mock(UserDetails.class);
		Mockito.when(userDetailsService.loadUserByUsername(userName)).thenReturn(userDetails);
		Mockito.when(jwtTokenUtil.generateToken(userDetails)).thenReturn(testToken);
		ResponseEntity<?> response;
		try {
			response = controller.login(authenticationRequest);
		} catch (Exception e) {
			Assertions.fail("Method login throws Exception");
			return;
		}
		Assertions.assertEquals(ResponseEntity.ok(new JwtResponse(testToken)).toString(), 
			response.toString(), "Unexpected behavior with token");
		
		Mockito.when(authenticationManager.authenticate(Mockito.any()))
			.thenThrow(new BadCredentialsException("bad credentials"))
			.thenThrow(new DisabledException("disabled login"));
		try {
			response = controller.login(authenticationRequest);
		} catch (Exception e) {
			Assertions.fail("Method login throws Exception");
			return;
		}
		Assertions.assertEquals(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(new Response("Username or password incorrect")).toString(), 
			response.toString(), "Unexpected behavior with bad credentials");
		try {
			response = controller.login(authenticationRequest);
		} catch (Exception e) {
			Assertions.fail("Method login throws Exception");
			return;
		}
		Assertions.assertEquals(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(new Response("Username or password incorrect")).toString(), 
			response.toString(), "Unexpected behavior with bad credentials");
	}
}
