package com.example.testfilament;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.app.UiAutomation;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import com.google.android.filament.Engine;
import com.google.android.filament.EntityManager;
import com.google.android.filament.Filament;
import com.google.android.filament.IndirectLight;
import com.google.android.filament.LightManager;
import com.google.android.filament.Scene;
import com.google.android.filament.Skybox;
import com.google.android.filament.gltfio.Animator;
import com.google.android.filament.gltfio.AssetLoader;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.android.filament.gltfio.MaterialProvider;
import com.google.android.filament.gltfio.ResourceLoader;
import com.google.android.filament.utils.KtxLoader;
import com.google.android.filament.utils.ModelViewer;
import com.google.android.filament.utils.Utils;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.util.function.Function;

public class MainActivity extends AppCompatActivity {


    private SurfaceView surfaceView;
    private Choreographer choreographer;
    //private val frameScheduler = FrameCallback()
    private ModelViewer modelViewer;

    private boolean animation = true;
    //private val doubleTapListener = DoubleTapListener()
    private GestureDetector doubleTapDetector;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //inizializzo filament
        //Filament.init();
        Utils.INSTANCE.init();

        surfaceView = new SurfaceView(this);
        setContentView(surfaceView);

        choreographer = Choreographer.getInstance();

        modelViewer = new ModelViewer(surfaceView);

        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                modelViewer.onTouch(view, motionEvent);
                return true;
            }
        });

        try {
            createRenderables();
            //createIndirectLight();
            loadEnvironment("venetian_crossroads_2k");

        } catch (IOException e) {
            e.printStackTrace();
        }


        com.google.android.filament.View.DynamicResolutionOptions dynamicResolutionOptions = modelViewer.getView().getDynamicResolutionOptions();
        dynamicResolutionOptions.enabled = true;
        dynamicResolutionOptions.homogeneousScaling = false;
        dynamicResolutionOptions.quality = com.google.android.filament.View.QualityLevel.HIGH;
        modelViewer.getView().setDynamicResolutionOptions(dynamicResolutionOptions);

        com.google.android.filament.View.AmbientOcclusionOptions ssaoOptions = modelViewer.getView().getAmbientOcclusionOptions();
        ssaoOptions.quality = com.google.android.filament.View.QualityLevel.HIGH;
        modelViewer.getView().setAmbientOcclusionOptions(ssaoOptions);

        com.google.android.filament.View.BloomOptions bloomOptions = modelViewer.getView().getBloomOptions();
        bloomOptions.enabled = true;
        modelViewer.getView().setBloomOptions(bloomOptions);
    }

    private void createRenderables() throws IOException {
        String path = "models/DamagedHelmet.glb";
        ByteBuffer bb = readCompressedAsset(path);

        modelViewer.loadModelGlb(bb);
        modelViewer.transformToUnitCube();

    }
    private void createIndirectLight() {
        Engine engine = modelViewer.getEngine();
        Scene scene = modelViewer.getScene();

        Skybox.Builder sb = new Skybox.Builder();
        IndirectLight.Builder ib = new IndirectLight.Builder();
        IndirectLight indiLight = ib.build(engine);
        indiLight.setIntensity(30_000f);
        scene.setSkybox(sb.build(engine));
        scene.setIndirectLight(indiLight);
    }

    private ByteBuffer readCompressedAsset(String assetName) throws IOException {
        InputStream input = getAssets().open(assetName);
        byte[] bytes = IOUtils.toByteArray(input);;

        input.read(bytes);
        return ByteBuffer.wrap(bytes);
    }

    private void loadEnvironment(String ibl) throws IOException {
        // Create the indirect light source and add it to the scene.
        KtxLoader.Options options = new KtxLoader.Options();
        ByteBuffer buffer = readCompressedAsset("envs/"+ibl+"/"+ibl+"_ibl.ktx");

        IndirectLight indirectLight = KtxLoader.INSTANCE.createIndirectLight(modelViewer.getEngine(), buffer, options);
        indirectLight.setIntensity(50_000f);
        modelViewer.getScene().setIndirectLight(indirectLight);

        // Create the sky box and add it to the scene.
        buffer = readCompressedAsset("envs/"+ibl+"/"+ibl+"_ibl.ktx");
        Skybox skybox = KtxLoader.INSTANCE.createSkybox(modelViewer.getEngine(), buffer, options);
        modelViewer.getScene().setSkybox(skybox);
    }

    Choreographer.FrameCallback frameScheduler = new Choreographer.FrameCallback() {
        private long startTime = System.nanoTime();
        @Override
        public void doFrame(long l) {
            choreographer.postFrameCallback(this);

            Animator animator = modelViewer.getAnimator();
            int animationCount = animator.getAnimationCount();

            if (animationCount > 0 && animation) {
                float elapsedTimeSeconds = (l - startTime)/ 1_000_000_000;
                animator.applyAnimation(0, elapsedTimeSeconds);
            }
            animator.updateBoneMatrices();

            modelViewer.render(l);
        }

    };

    @Override
    public void onResume() {
        super.onResume();
        choreographer.postFrameCallback(frameScheduler);
    }

    @Override
    public void onPause() {
        super.onPause();

        choreographer.removeFrameCallback(frameScheduler);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        choreographer.removeFrameCallback(frameScheduler);
    }
}
