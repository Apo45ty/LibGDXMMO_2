package games.apolion;

import static com.badlogic.gdx.Gdx.files;
import static com.badlogic.gdx.Gdx.input;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;

import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.EnvironmentUtil;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

import games.apolion.controls.MyFirstPersonCameraController;
import games.apolion.textchat.ChatClient;
import games.apolion.textchat.ChatClientHandeler;
import games.apolion.textchat.ChatServer;

public class Main extends ApplicationAdapter {


	private static final boolean IS_DEBUG = true;
	private Stage stage;
	private Skin skin;
	private TextButton sendButton;
	private ChatClient client;
	private TextField textField;
	private TextArea textArea;
	private TextButton startServerButton;
	private TextButton startClientButton;
	private int port=9000;
	private Thread tClient;

	private SceneManager sceneManager;
	private SceneAsset sceneAsset;
	private Scene scene;
	private PerspectiveCamera camera;
	private Cubemap diffuseCubemap;
	private Cubemap environmentCubemap;
	private Cubemap specularCubemap;
	private Texture brdfLUT;
	private float time;
	private SceneSkybox skybox;
	private DirectionalLightEx light;
	private FirstPersonCameraController fpcc;


	@Override
	public void create () {
		// create scene
		sceneAsset = new GLBLoader().load(Gdx.files.internal("Glob.glb"));
		scene = new Scene(sceneAsset.scene);
		sceneManager = new SceneManager();
		sceneManager.addScene(scene);

		// setup camera (The BoomBox model is very small so you may need to adapt camera settings for your scene)
		camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		float d = .02f;
		camera.near = d / 1000f;
		camera.far = 200;
		sceneManager.setCamera(camera);
		fpcc= new MyFirstPersonCameraController(camera);

		// setup light
		light = new DirectionalLightEx();
		light.direction.set(1, -3, 1).nor();
		light.color.set(Color.WHITE);
		sceneManager.environment.add(light);

		// setup quick IBL (image based lighting)
		IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);
		environmentCubemap = iblBuilder.buildEnvMap(1024);
		diffuseCubemap = iblBuilder.buildIrradianceMap(256);
		specularCubemap = iblBuilder.buildRadianceMap(10);
		iblBuilder.dispose();

		// This texture is provided by the library, no need to have it in your assets.
		brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

		sceneManager.setAmbientLight(1f);
		sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
		sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
		sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));

		// setup skybox
		skybox = new SceneSkybox(environmentCubemap);
		sceneManager.setSkyBox(skybox);

		/*******************************************************************
		 *  UI
		 *******************************************************************/

		//Setup Stage and globals
		stage = new Stage();
		input.setInputProcessor(stage);

		//Setup UI
		skin = new Skin(files.internal("uiskin.json"));
		textArea = new TextArea(
				"",
				skin);
		textArea.setDisabled(true);
		textArea.setX(10);
		textArea.setY(30);
		textArea.setWidth(200);
		textArea.setHeight(200);
		textField = new TextField("Text field", skin);
		textField.setX(10);
		textField.setY(250);
		textField.setWidth(200);
		textField.setHeight(30);
		stage.addActor(textArea);
		stage.addActor(textField);
		TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
		textButtonStyle.font = new BitmapFont();
		textButtonStyle.fontColor = Color.WHITE;
		textButtonStyle.overFontColor = Color.BLACK;
		sendButton = new TextButton("Send Message ", textButtonStyle);
		sendButton.setX(10);
		sendButton.setY(10);
		stage.addActor(sendButton);

		//LISTENERS
		sendButton.addListener(new ClickListener(){
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				System.out.println("Send Pressed:"+textField.getText());
				client.setInputString(textField.getText());
			}
		});

		InputMultiplexer multiplexer = new InputMultiplexer();
		Gdx.input.setInputProcessor(multiplexer);
		multiplexer.addProcessor(fpcc);
		multiplexer.addProcessor(stage);

		//DEBUG
		if(IS_DEBUG){
			System.out.println("Debug Option Start Server And Chat Client");
			ChatServer.StartServer(port);
			AddClientToChat("localhost",port);
			client.username = "debug";

			//INIT UI
			startServerButton = new TextButton("Start Server", textButtonStyle);
			startServerButton.setX(10);
			startServerButton.setY(320);
			stage.addActor(startServerButton);
			startClientButton = new TextButton("Start client", textButtonStyle);
			startClientButton.setX(10);
			startClientButton.setY(290);
			stage.addActor(startClientButton);

			//LISTENERS
			startServerButton.addListener(new ClickListener(){
				@Override
				public void clicked(InputEvent event, float x, float y) {
					super.clicked(event, x, y);
					//SetupChat
					try{
						System.out.println("Server Started at port "+ port);
						ChatServer.StartServer(port);
					}catch (Exception e){

					}
				}
			});
			startClientButton.addListener(new ClickListener(){
				@Override
				public void clicked(InputEvent event, float x, float y) {
					super.clicked(event, x, y);
					System.out.println("Client joined");
					AddClientToChat("localhost",port);
				}
			});
		}
	}

	private void AddClientToChat(String host, int port) {
		client = new ChatClient(host,port);
		tClient = new Thread(client);
		tClient.start();
	}

	@Override
	public void resize(int width, int height) {
		sceneManager.updateViewport(width, height);
	}

	@Override
	public void render () {
		super.render();
		ScreenUtils.clear(0, 0, 0, 0);
		float deltaTime = Gdx.graphics.getDeltaTime();
		time += deltaTime;

		fpcc.update();

		// render
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		sceneManager.update(deltaTime);
		sceneManager.render();

		//DrawUI
		stage.draw();
		textArea.appendText( ChatClientHandeler.output);
		ChatClientHandeler.output="";
	}
	
	@Override
	public void dispose () {sceneManager.dispose();
		sceneManager.dispose();
		sceneAsset.dispose();
		environmentCubemap.dispose();
		diffuseCubemap.dispose();
		specularCubemap.dispose();
		brdfLUT.dispose();
		skybox.dispose();

		try {
			tClient.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
