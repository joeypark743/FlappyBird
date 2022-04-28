import com.badlogic.gdx.ApplicationAdapter; 
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer; 
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle; 
import com.badlogic.gdx.math.Circle; 
import com.badlogic.gdx.Input.Keys; 
import com.badlogic.gdx.math.Vector2; 
import com.badlogic.gdx.math.MathUtils; 
import com.badlogic.gdx.math.Intersector; 
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.*; 
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;

public class FlappyBird extends ApplicationAdapter
{
    private OrthographicCamera camera; //the camera to our world
    private Viewport viewport; //maintains the ratios of your world
    private ShapeRenderer renderer; //used to draw textures and fonts 
    private BitmapFont font; //used to draw fonts (text)
    private SpriteBatch batch; //also needed to draw fonts (text)
    private GlyphLayout layout; //needed to get the width and height of our text message

    private Array<Rectangle> tubes; 

    private Circle bird;  
    private float velY; 
    private boolean started; 
    private boolean menu;
    private float time; 

    private Vector2 mousePos;
    private Circle circleMouse;
    private Circle option1;
    private Circle option2;
    private Circle option3;

    private Array<Texture> flyingBird;
    private int ctr;
    private int halfSec;
    private int score;

    private Music music;
    private Sound sound;

    public static final float WORLD_WIDTH = 600; 
    public static final float WORLD_HEIGHT = 800;
    public static final float TUBE_WIDTH = 100;
    public static final float TUBE_BUFFER = 70;//buffer between the top and bottom of the screen
    //so tubes aren't created too high or too low
    public static final float TUBE_GAP_VERTICAL = 210; //gap between the tubes

    public static final float RADIUS = 35;
    public static final float SCROLL_SPEED = 4;//how fast the tubes move across the screen
    public static final float SPAWN_RATE = 1.8f;//how long it takes to spawn another set of tubes
    public static final float GRAVITY = WORLD_HEIGHT;
    public static final float JUMP_SPEED = WORLD_HEIGHT * 30f; 

    @Override//called once when we start the game
    public void create(){
        camera = new OrthographicCamera(); 
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera); 
        renderer = new ShapeRenderer(); 
        font = new BitmapFont(); 
        batch = new SpriteBatch();//if you want to use images instead of using ShapeRenderer, also needed for text 
        layout = new GlyphLayout(); 

        circleMouse = new Circle(-1, -1, 1);
        mousePos = new Vector2();
        option1 = new Circle((WORLD_WIDTH / 2),(WORLD_HEIGHT / 2), RADIUS); 

        started = false; 
        menu = true;

        //generates a random y value for the bottom left of the top tube
        float y = generateRandomY(); 
        tubes = new Array<Rectangle>(); 

        //create the top tube rectangle
        tubes.add(new Rectangle(WORLD_WIDTH, y, TUBE_WIDTH, WORLD_HEIGHT - y)); 
        //create the bottom tube rectangle
        tubes.add(new Rectangle(WORLD_WIDTH, 0, TUBE_WIDTH, y - TUBE_GAP_VERTICAL)); 

        bird = new Circle(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, RADIUS); 
        velY = 0; //y velocity to add onto the bird whether it is jumping or falling due to gravity
        time = 0; //keep track of the time that has passed to spawn a new tube

        sound = Gdx.audio.newSound(Gdx.files.internal("sound.mp3"));
        music = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));
        music.setVolume((float)(0.2));
        sound.setVolume(4, (float)2);
        music.setLooping(true);
        flyingBird = new Array<Texture>();
        ctr = 0;
        halfSec = 0;

        flyingBird.add(new Texture(Gdx.files.internal("bird1.png")));
        flyingBird.add(new Texture(Gdx.files.internal("bird2.png")));
        flyingBird.add(new Texture(Gdx.files.internal("bird3.png")));
        flyingBird.add(new Texture(Gdx.files.internal("bird4.png")));
        score = 0;

    }
    @Override//called 60 times a second
    public void render(){
        ctr++;
        if(ctr % 30 == 0)
        {
            halfSec++;
        }
        viewport.apply(); 
        

        Gdx.gl.glClearColor(0, 0, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        float delta = Gdx.graphics.getDeltaTime();//1/60 

        float y = 0;
        getInput();

        
        if(started)
        {
            music.play();
            time += delta; //add on 1/60 every time render is called

            //update the tubes
            for(Rectangle tube : tubes)
            {
                tube.x -= SCROLL_SPEED;    
                if(tube.x == bird.x)
                {
                    score++;
                }
            }

            //remove the tubes from the list if they go off the screen
            for(int i = 0; i < tubes.size; i++)
            {
                Rectangle tube = tubes.get(i);
                if(tube.x + TUBE_WIDTH < 0)
                {
                    tubes.removeIndex(i); 
                    i--;
                }
            }

            //if a certain amount of time has passed create a new tube
            if(time > SPAWN_RATE)
            {
                //create a new top and bottome Rectangle and reset the time
                time = 0;
                y = generateRandomY();
                tubes.add(new Rectangle(WORLD_WIDTH, y, TUBE_WIDTH, WORLD_HEIGHT - y)); 
                tubes.add(new Rectangle(WORLD_WIDTH, 0, TUBE_WIDTH, y - TUBE_GAP_VERTICAL));
            }

            //add GRAVITY to the y velocity so it it increases, causing the bird to fall faster
            velY -= GRAVITY * delta;
            if(Gdx.input.isKeyJustPressed(Keys.SPACE))
            {
                velY = JUMP_SPEED * delta;//set the y velocity to the JUMP_SPEED if SPACE is pressed
                sound.play();

            }
            bird.y += velY * delta; //change the y position of the bird based on the y velocity

            //TODO: loop through the tubes Array and check if any of the Rectangles overlaps
            //with the bird. Use the static method from the Intersector class, overlaps, 
            //that has two parameters the first a Circle object and the second a Rectangle object
            //Intersector.overlaps(_Circle_, _Rectangle_)
            //if the intersect reset the game: started to false, the bird back to the middle of 
            //the screen, velY to 0, and clear all the Rectangle objects from tubes Array. 

            for(Rectangle element: tubes)
            {
                if(Intersector.overlaps(bird, element))
                {
                    started = false;
                    menu = true;
                    bird.setX(WORLD_WIDTH / 2);
                    bird.setY(WORLD_HEIGHT / 2);
                    velY = 0;
                    tubes.clear();
                    sound.stop();

                }
            }

            //TODO check if the bird goes too high or too low. If it does, reset the game: 
            //started to false, the bird back to the middle of 
            //the screen, velY to 0, and clear all the Rectangle objects from tubes. 
            if(bird.y + RADIUS >= WORLD_HEIGHT || bird.y - RADIUS <= 0)
            {
                started = false;
                menu = true;
                bird.setX(WORLD_WIDTH / 2);
                bird.setY(WORLD_HEIGHT / 2);
                velY = 0;
                tubes.clear();
                sound.stop();
            }

            renderer.setProjectionMatrix(viewport.getCamera().combined);
            renderer.setColor(Color.WHITE); 
            renderer.begin(ShapeType.Filled);
            

            //TODO: draw the tubes rectangles and the bird (circle)
           
            for(Rectangle element: tubes)
            {
                renderer.rect(element.x, element.y, element.width, element.height);
            }
            
            renderer.end();
            
            batch.setProjectionMatrix(viewport.getCamera().combined);
            batch.begin();
            
            if(halfSec % 4 == 1)
            {
                batch.draw(flyingBird.get(0),(float) bird.x - RADIUS, (float)bird.y-RADIUS, bird.radius * 2, bird.radius * 2);
            }
            else if(halfSec % 4 == 2)
            {
                batch.draw(flyingBird.get(1),(float) bird.x - RADIUS, (float)bird.y-RADIUS, bird.radius * 2, bird.radius * 2);
            }
            else if(halfSec % 4 == 3)
            {
                batch.draw(flyingBird.get(2),(float) bird.x - RADIUS, (float)bird.y-RADIUS, bird.radius * 2, bird.radius * 2);
            }
            else if(halfSec % 4 == 0)
            {
                batch.draw(flyingBird.get(3),(float) bird.x - RADIUS, (float)bird.y-RADIUS, bird.radius * 2, bird.radius * 2);
            }
            batch.end();
            
            batch.setProjectionMatrix(viewport.getCamera().combined);
            batch.begin();
            layout.setText(font, "" + score / 2);
            font.setColor(Color.GREEN);
            font.draw(batch, layout, 30, WORLD_HEIGHT - 50);
            
            
            batch.end();

        }
        if(menu)
        {
            score = 0;
            ctr = 0;
            halfSec = 0;
            music.stop();
            renderer.setProjectionMatrix(viewport.getCamera().combined);
            renderer.setColor(Color.DARK_GRAY); 
            renderer.begin(ShapeType.Filled);

            renderer.circle(option1.x, option1.y, option1.radius);

            renderer.end();

            batch.setProjectionMatrix(viewport.getCamera().combined);
            batch.begin();

            layout.setText(font, "Normal");
            font.setColor(Color.WHITE);
            font.draw(batch, layout, option1.x - (RADIUS / 2) - 5, option1.y + 10);

            layout.setText(font, "Choose a difficulty and SPACE to jump");
            font.draw(batch, layout, 
                WORLD_WIDTH / 2 - layout.width / 2, 
                (WORLD_HEIGHT* 3) / 4);
            batch.end(); 
        }
        //draw everything on the screen

    }
    private void getInput()
    {
        int x = Gdx.input.getX();
        int y = Gdx.input.getY();
        mousePos = viewport.unproject(new Vector2(x,y));
        circleMouse.setPosition(mousePos);
        if(Intersector.overlaps(circleMouse, option1) && Gdx.input.justTouched())
        {
            started = true;
            menu = false;

        }
    }

    public float generateRandomY()
    {
        //TODO: return a random y value from TUBE_BUFFER + TUBE_GAP_VAERTICAL to WORLD_HEIGHT - TUBE_BUFFER
        //Look up the random method from the MathUtils class in the libgdx library
        return (float)Math.random()* ((TUBE_BUFFER + TUBE_GAP_VERTICAL) - (WORLD_HEIGHT - TUBE_BUFFER) + 1) + (WORLD_HEIGHT - TUBE_BUFFER);

    }

    @Override
    public void resize(int width, int height){
        viewport.update(width, height, true); 
    }

    @Override
    public void dispose(){
        renderer.dispose(); 
        batch.dispose(); 
    }
    
    
    

}
