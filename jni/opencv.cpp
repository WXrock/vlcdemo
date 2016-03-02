#include <opencv.h>
#include<android/log.h>
//#include <opencv2/core/core.hpp>
#include <string>
#include <vector>
#include <fstream>
#include <opencv2/opencv_modules.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/stitching/detail/autocalib.hpp>
#include <opencv2/stitching/detail/blenders.hpp>
#include <opencv2/stitching/detail/camera.hpp>
#include <opencv2/stitching/detail/exposure_compensate.hpp>
#include <opencv2/stitching/detail/matchers.hpp>
#include <opencv2/stitching/detail/motion_estimators.hpp>
#include <opencv2/stitching/detail/seam_finders.hpp>
#include <opencv2/stitching/detail/util.hpp>
#include <opencv2/stitching/detail/warpers.hpp>
#include <opencv2/stitching/warpers.hpp>
#include <cv.h>

#define  LOG_TAG    "libopencv"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

using namespace cv;
using namespace std;
using namespace cv::detail;

#define START  220
#define END 220
#define COMPOSE_MEGAPIX_PREVIEW 0.6f
#define WORK_MEGAPIX_PREVIEW 0.6f
#define STEP 0.15f
#define MIN_CONF 0.3f

//定义参数  
vector<string> img_names;  
bool try_gpu = false;  // 是否使用GPU(图形处理器)，默认为no
bool preview = true;
bool auto_thresh = false;
double totaltime;

/* 运动估计参数 */
double work_megapix = -1;//<--work_megapix <float>> 图像匹配的分辨率大小，
						//图像的面积尺寸变为work_megapix*100000，不缩放时为-1   
float conf_thresh = 1.f;//conf_thresh <float>两幅图来自同一全景图的置信度  
WaveCorrectKind wave_correct = detail::WAVE_CORRECT_HORIZ;//wave_correct (no|horiz|vert) 波形校验(水平，垂直或者没有),
															//默认是horiz  
float match_conf = 0.4f;//match_conf <float> 特征点检测置信等级，最近邻匹配距离与次近邻匹配距离的比值，
						//surf默认为0.65，orb默认为0.3 

/*图像融合参数*/
double seam_megapix = 0.1;//seam_megapix <double> 拼接缝像素的大小，默认为0.1 
double compose_megapix = -1;//compose_megapix <double>拼接分辨率，不缩放时为-1
int expos_comp_type = ExposureCompensator::GAIN_BLOCKS;//expos_comp (no|gain|gain_blocks)光照补偿方法，默认是gain_blocks
int blend_type = Blender::MULTI_BAND;//blend (no|feather|multiband) 融合方法，默认是多频段融合  
float blend_strength = 5;//融合强度，0 - 100.默认是5. 

char* jstringTostring(JNIEnv* env, jstring jstr) 
{
       char* rtn = NULL;
       jclass clsstring = env->FindClass("java/lang/String");
       jstring strencode = env->NewStringUTF("utf-8");
       jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
       jbyteArray barr= (jbyteArray)env->CallObjectMethod(jstr, mid, strencode);
       jsize alen = env->GetArrayLength(barr);
       jbyte* ba = env->GetByteArrayElements(barr, JNI_FALSE);
       if (alen > 0)
       {
                 rtn = (char*)malloc(alen + 1);
                 memcpy(rtn, ba, alen);
                 rtn[alen] = 0;
       }
       env->ReleaseByteArrayElements(barr, ba, 0);
       return rtn;
}


jstring stoJstring(JNIEnv* env, const char* pat)
{
	jclass strClass = env->FindClass("java/lang/String");
	jmethodID ctorID = env->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
	jbyteArray bytes = env->NewByteArray(strlen(pat));
	env->SetByteArrayRegion(bytes, 0, strlen(pat), (jbyte*)pat);
	jstring encoding = env->NewStringUTF("utf-8");
	return (jstring)env->NewObject(strClass, ctorID, bytes, encoding);
} 

std::string jstring2str(JNIEnv* env, jstring jstr)  
{     
    char*   rtn   =   NULL;     
    jclass   clsstring   =   env->FindClass("java/lang/String");     
    jstring   strencode   =   env->NewStringUTF("GB2312");     
    jmethodID   mid   =   env->GetMethodID(clsstring,   "getBytes",   "(Ljava/lang/String;)[B");     
    jbyteArray   barr=   (jbyteArray)env->CallObjectMethod(jstr,mid,strencode);     
    jsize   alen   =   env->GetArrayLength(barr);     
    jbyte*   ba   =   env->GetByteArrayElements(barr,JNI_FALSE);     
    if(alen   >   0)     
    {     
        rtn   =   (char*)malloc(alen+1);           
        memcpy(rtn,ba,alen);     
        rtn[alen]=0;     
    }     
    env->ReleaseByteArrayElements(barr,ba,0);     
    std::string stemp(rtn);  
    free(rtn); 
    return   stemp;     
}   


JNIEXPORT jint JNICALL Java_com_example_vlcdemo_ImageProc_proc
		(JNIEnv *env, jclass obj,jstring path,jint pstart,jint pnum,jfloat mconf_thresh,jboolean isAuto)
{
        int64 start,finish;
        start = getTickCount();
	conf_thresh = (float)mconf_thresh;
        auto_thresh = (bool)isAuto;
	if(preview){
		compose_megapix = COMPOSE_MEGAPIX_PREVIEW;
		//work_megapix = WORK_MEGAPIX_PREVIEW;
		LOGLN("preview mode is set!!!!!");
	}
	
    //pushback picture names
    int i;
	char tmp;
   // const char *charPath = jstringTostring(env,path);
	string strPath = jstring2str(env,path);

    for(i= pstart;i<pstart+pnum;i++) {
		tmp = '0';
		tmp += (int)i;
        img_names.push_back(strPath+tmp+".jpg");
    }
    string result_name = strPath + "result.jpg";
    LOGE("%s\n",result_name.c_str());

    int num_images = (int)pnum;
    double work_scale = 1, seam_scale = 1, compose_scale = 1;
    bool is_work_scale_set = false, is_seam_scale_set = false, is_compose_scale_set = false;

    LOGI("Finding features...\n");

    Ptr<FeaturesFinder> finder;
    finder = new SurfFeaturesFinder();
    
	vector<Mat> full_img(num_images);
	vector<Mat> img(num_images);
    vector<ImageFeatures> features(num_images);
    vector<Mat> images(num_images);
    vector<Size> full_img_sizes(num_images);
    double seam_work_aspect = 1;


    #pragma omp parallel for
    for (int i = 0; i < num_images; ++i)
    {
        full_img[i] = imread(img_names[i]);
        full_img_sizes[i] = full_img[i].size();

        if (work_megapix < 0)
        {
            img[i] = full_img[i];
            work_scale = 1;
            is_work_scale_set = true;
        }
        else
        {
            if (!is_work_scale_set)
            {
                work_scale = min(1.0, sqrt(work_megapix * 1e6 / full_img[i].size().area()));
                is_work_scale_set = true;
            }
            resize(full_img[i], img[i], Size(), work_scale, work_scale);
        }
        if (!is_seam_scale_set)
        {
            seam_scale = min(1.0, sqrt(seam_megapix * 1e6 / full_img[i].size().area()));
            seam_work_aspect = seam_scale / work_scale;
            is_seam_scale_set = true;
        }

        (*finder)(img[i], features[i]);
        features[i].img_idx = i;
        LOGI("Features in image #%d:%d\n",i+1,features[i].keypoints.size());

        resize(full_img[i], img[i], Size(), seam_scale, seam_scale);
        images[i] = img[i].clone();
    }

    finder->collectGarbage();
    //full_img.release();
    //img.release();

    LOG("Pairwise matching\n");

    vector<MatchesInfo> pairwise_matches;
	BestOf2NearestMatcher matcher(try_gpu, match_conf);
	
	Mat matchMask(features.size(),features.size(),CV_8U,Scalar(0));
	for (int i = 0; i < num_images-1 ; ++i)
	{
		matchMask.at<char>(i,i+1) =1;
	}
	matchMask.at<char>(0,num_images-1) = 1;
	matcher(features, pairwise_matches,matchMask);
	matcher.collectGarbage();
	
	const vector<MatchesInfo> pairwise_matches_backup = pairwise_matches;
	const vector<ImageFeatures> features_backup = features;


    // Leave only images we are sure are from the same panorama
    vector<int> indices = leaveBiggestComponent(features, pairwise_matches, conf_thresh);
    if(auto_thresh)
	while(indices.size()<num_images){
		LOGI("current conf_thresh is %f,left :%d,try to decrease..\n", conf_thresh,indices.size());
		conf_thresh -= STEP;
		if(conf_thresh < MIN_CONF){
			conf_thresh += STEP;
			//cout << matchesGraphAsString(img_names, pairwise_matches, conf_thresh);
			LOGI("CONF_THRESH is too low:%f,exit\n",conf_thresh);
			break;
		}
		pairwise_matches = pairwise_matches_backup;
		features = features_backup;
		indices = leaveBiggestComponent(features, pairwise_matches, conf_thresh);
	}

	LOGI("set conf_thresh as :%f\n", conf_thresh);
	// Check if we should save matches graph
    //cout << matchesGraphAsString(img_names, pairwise_matches, conf_thresh);

    vector<Mat> img_subset;
    vector<string> img_names_subset;
    vector<Size> full_img_sizes_subset;
    for (size_t i = 0; i < indices.size(); ++i)
    {
        img_names_subset.push_back(img_names[indices[i]]);
        img_subset.push_back(images[indices[i]]);
        full_img_sizes_subset.push_back(full_img_sizes[indices[i]]);
    }

    images = img_subset;
    img_names = img_names_subset;
    full_img_sizes = full_img_sizes_subset;

    // Check if we still have enough images
    num_images = static_cast<int>(img_names.size());
	LOGI("left:%d images\n",num_images);

    HomographyBasedEstimator estimator;
    vector<CameraParams> cameras;
    estimator(features, pairwise_matches, cameras);

    for (size_t i = 0; i < cameras.size(); ++i)
    {
        Mat R;
        cameras[i].R.convertTo(R, CV_32F);
        cameras[i].R = R;
        LOGI("Initial intrinsics #%d\n",indices[i]+1);
    }

	Ptr<detail::BundleAdjusterBase> adjuster;//光束调整器参数  
	adjuster = new detail::BundleAdjusterRay();//使用Bundle Adjustment（光束法平差）方法对所有图片进行相机参数校正  

	adjuster->setConfThresh(conf_thresh);//设置配置阈值  
	Mat_<uchar> refine_mask = Mat::zeros(3, 3, CV_8U);  
	refine_mask(0,0) = 1;  
	refine_mask(0,1) = 1;  
	refine_mask(0,2) = 1;  
	refine_mask(1,1) = 1;  
	refine_mask(1,2) = 1;  
	adjuster->setRefinementMask(refine_mask);  
	(*adjuster)(features, pairwise_matches, cameras);//进行矫正  
   
	// Find median focal length
    vector<double> focals;
    for (size_t i = 0; i < cameras.size(); ++i)
    {
        LOGI("Camera #%d\n",indices[i]+1);
        focals.push_back(cameras[i].focal);
    }

    sort(focals.begin(), focals.end());
    float warped_image_scale;
    if (focals.size() % 2 == 1)
        warped_image_scale = static_cast<float>(focals[focals.size() / 2]);
    else
        warped_image_scale = static_cast<float>(focals[focals.size() / 2 - 1] + focals[focals.size() / 2]) * 0.5f;


    vector<Mat> rmats;
    for (size_t i = 0; i < cameras.size(); ++i)
        rmats.push_back(cameras[i].R);
    waveCorrect(rmats, wave_correct);
    for (size_t i = 0; i < cameras.size(); ++i)
        cameras[i].R = rmats[i];

    LOGI("Warping images (auxiliary)... \n");


    vector<Point> corners(num_images);
    vector<Mat> masks_warped(num_images);
    vector<Mat> images_warped(num_images);
    vector<Size> sizes(num_images);
    vector<Mat> masks(num_images);

    // Preapre images masks
    for (int i = 0; i < num_images; ++i)
    {
        masks[i].create(images[i].size(), CV_8U);
        masks[i].setTo(Scalar::all(255));
    }

    // Warp images and their masks

    Ptr<WarperCreator> warper_creator;
	warper_creator = new cv::CylindricalWarper();

    Ptr<RotationWarper> warper = warper_creator->create(static_cast<float>(warped_image_scale * seam_work_aspect));

    for (int i = 0; i < num_images; ++i)
    {
        Mat_<float> K;
        cameras[i].K().convertTo(K, CV_32F);
        float swa = (float)seam_work_aspect;
        K(0,0) *= swa; K(0,2) *= swa;
        K(1,1) *= swa; K(1,2) *= swa;

        corners[i] = warper->warp(images[i], K, cameras[i].R, INTER_LINEAR, BORDER_REFLECT, images_warped[i]);
        sizes[i] = images_warped[i].size();

        warper->warp(masks[i], K, cameras[i].R, INTER_NEAREST, BORDER_CONSTANT, masks_warped[i]);
    }

    vector<Mat> images_warped_f(num_images);
    for (int i = 0; i < num_images; ++i)
        images_warped[i].convertTo(images_warped_f[i], CV_32F); //move to first for loop


    Ptr<ExposureCompensator> compensator = ExposureCompensator::createDefault(expos_comp_type);
    compensator->feed(corners, images_warped, masks_warped);

    Ptr<SeamFinder> seam_finder;
	seam_finder = new detail::DpSeamFinder(DpSeamFinder::COLOR_GRAD);
    seam_finder->find(images_warped_f, corners, masks_warped);

    // Release unused memory
    images.clear();
    images_warped.clear();
    images_warped_f.clear();
    masks.clear();

    LOGI("Compositing...\n");
#if ENABLE_LOG
    t = getTickCount();
#endif

    Mat img_warped, img_warped_s;
    Mat dilated_mask, seam_mask, mask, mask_warped;
    Ptr<Blender> blender;
    //double compose_seam_aspect = 1;
    double compose_work_aspect = 1;


	if (!is_compose_scale_set)
    {
        if (compose_megapix > 0)
            compose_scale = min(1.0, sqrt(compose_megapix * 1e6 / full_img[0].size().area()));
        is_compose_scale_set = true;

        // Compute relative scales
        //compose_seam_aspect = compose_scale / seam_scale;
        compose_work_aspect = compose_scale / work_scale;

        // Update warped image scale
        warped_image_scale *= static_cast<float>(compose_work_aspect);
        warper = warper_creator->create(warped_image_scale);

        // Update corners and sizes
        for (int i = 0; i < num_images; ++i)
        {
            // Update intrinsics
            cameras[i].focal *= compose_work_aspect;
            cameras[i].ppx *= compose_work_aspect;
            cameras[i].ppy *= compose_work_aspect;

            // Update corner and size
            Size sz = full_img_sizes[i];
            if (std::abs(compose_scale - 1) > 1e-1)
            {
                sz.width = cvRound(full_img_sizes[i].width * compose_scale);
                sz.height = cvRound(full_img_sizes[i].height * compose_scale);
            }

            Mat K;
            cameras[i].K().convertTo(K, CV_32F);
            Rect roi = warper->warpRoi(sz, K, cameras[i].R);
            corners[i] = roi.tl();
            sizes[i] = roi.size();
        }
    }

	if (blender.empty())
    {
        blender = Blender::createDefault(blend_type, try_gpu);
        Size dst_sz = resultRoi(corners, sizes).size();
        float blend_width = sqrt(static_cast<float>(dst_sz.area())) * blend_strength / 100.f;
        if (blend_width < 1.f)
            blender = Blender::createDefault(Blender::NO, try_gpu);
        else if (blend_type == Blender::MULTI_BAND)
        {
            MultiBandBlender* mb = dynamic_cast<MultiBandBlender*>(static_cast<Blender*>(blender));
            mb->setNumBands(static_cast<int>(ceil(log(blend_width)/log(2.)) - 1.));
        }
        else if (blend_type == Blender::FEATHER)
        {
            FeatherBlender* fb = dynamic_cast<FeatherBlender*>(static_cast<Blender*>(blender));
            fb->setSharpness(1.f/blend_width);
        }
        blender->prepare(corners, sizes);
    }

	//#pragma omp parallel for
    for (int img_idx = 0; img_idx < num_images; ++img_idx)
    {
        LOGI("Compositing image #%d\n",indices[img_idx]+1);

        // Read image and resize it if necessary
        //full_img = imread(img_names[img_idx]);
        
        if (abs(compose_scale - 1) > 1e-1)
            resize(full_img[img_idx], img[img_idx], Size(), compose_scale, compose_scale);
        else
            img[img_idx] = full_img[img_idx];

        full_img[img_idx].release();
        Size img_size = img[img_idx].size();

        Mat K;
        cameras[img_idx].K().convertTo(K, CV_32F);

        // Warp the current image
        warper->warp(img[img_idx], K, cameras[img_idx].R, INTER_LINEAR, BORDER_REFLECT, img_warped);

        // Warp the current image mask
        mask.create(img_size, CV_8U);
        mask.setTo(Scalar::all(255));
        warper->warp(mask, K, cameras[img_idx].R, INTER_NEAREST, BORDER_CONSTANT, mask_warped);

        // Compensate exposure
        compensator->apply(img_idx, corners[img_idx], img_warped, mask_warped);

        img_warped.convertTo(img_warped_s, CV_16S);
        img_warped.release();
        img[img_idx].release();
        mask.release();

        dilate(masks_warped[img_idx], dilated_mask, Mat());
        resize(dilated_mask, seam_mask, mask_warped.size());
        mask_warped = seam_mask & mask_warped;     

        // Blend the current image
        blender->feed(img_warped_s, mask_warped, corners[img_idx]);
    }

    Mat result, result_mask;
    blender->blend(result, result_mask);
	if(result.size().width == 1){
		LOGI("stitch failed,conf_thresh too low\n");
		return -1;
	}


	//cat image
	Range R;
	if(!preview){
		R.start = START;
		R.end = result.size().height - END;
	}else{
		R.start = (int)START*compose_megapix;
		R.end = result.size().height - (int)END*compose_megapix;
	}
	
    Mat final_pic = Mat(result,R,Range::all());
    imwrite(result_name, final_pic);

    finish=getTickCount();
    totaltime=(double)((finish-start)/getTickFrequency());
    LOGI("TOTAL TIME IS %f\n",totaltime);

    //release
    vector<string>().swap(img_names);
    return 0;
}

JNIEXPORT jdouble JNICALL Java_com_example_vlcdemo_ImageProc_getTime
                (JNIEnv *env, jclass obj)
{
    return (jdouble)totaltime;
}



