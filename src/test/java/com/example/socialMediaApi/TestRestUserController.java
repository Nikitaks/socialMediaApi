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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.socialMediaApi.controller.RestUserController;
import com.example.socialMediaApi.entity.Response;
import com.example.socialMediaApi.entity.User;
import com.example.socialMediaApi.repo.UserRepository;


@ExtendWith(MockitoExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class TestRestUserController {

	private final String userName = "AnyUserName";
	private final String encodedPassword = "AnyEncodedPassword";
	private final RestUserController controller = new RestUserController();
	private final User user = new User(userName, "password", "email@domain.com");

	private Authentication authenticationWithName;
	private UserRepository userRepository;
	private PasswordEncoder passwordEncoder;

	@BeforeAll
	public void beforeEach()  {
		authenticationWithName = Mockito.mock(Authentication.class);
		Mockito.when(authenticationWithName.getName()).thenReturn(userName);
		userRepository = Mockito.mock(UserRepository.class);
		controller.setUserRepository(userRepository);
		PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
		controller.setPasswordEncoder(passwordEncoder);
		Mockito.doReturn(encodedPassword).when(passwordEncoder).encode(user.getPassword());
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
	}
}
