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

//定义参数
vector<string> img_names;
//std::string result_name;
bool try_gpu = false;  // 是否使用GPU(图形处理器)，默认为no

/* 运动估计参数 */
double work_megapix = 0.6;//<--work_megapix <float>> 图像匹配的分辨率大小，
			//图像的面积尺寸变为work_megapix*100000，默认为0.6
float conf_thresh = 1.f;//conf_thresh <float>两幅图来自同一全景图的置信度
WaveCorrectKind wave_correct = detail::WAVE_CORRECT_HORIZ;//wave_correct (no|horiz|vert) 波形校验(水平，垂直或者没有),
							//默认是horiz
float match_conf;//match_conf <float> 特征点检测置信等级，最近邻匹配距离与次近邻匹配距离的比值，
						//surf默认为0.65，orb默认为0.3
/*图像融合参数*/
double seam_megapix = 0.1;//seam_megapix <double> 拼接缝像素的大小，默认为0.1 
double compose_megapix =0.6;////compose_megapix <double>拼接分辨率，默认为-1
int expos_comp_type = ExposureCompensator::GAIN_BLOCKS;//expos_comp (no|gain|gain_blocks)光照补偿方法，默认是gain_blocks
int blend_type = Blender::MULTI_BAND;//blend (no|feather|multiband) 融合方法，默认是多频段融合
float blend_strength = 5;//融合强度，0 - 100.默认是5.
double totaltime;
//string result_name = "sdcard/result.jpg";//输出图像的文件名

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
		(JNIEnv *env, jclass obj,jstring path,jfloat var,jint width,jint height)
{
    clock_t start,finish;
    start=clock();

    int mwidth = (int)(width * 0.625);
    int mheight = (int)(height * 0.625);
    match_conf = (float)var;

    int i;
    char resultBuf[100];
    char num_tmp;
    string pathBuf[8];
    const char *charPath = jstringTostring(env,path);

    for(i= 0;i<8;i++) {
        sprintf(&num_tmp,"%d",i);
        LOGE("%c\n",num_tmp);
        //strcat
        pathBuf[i] += charPath;
        pathBuf[i] += num_tmp;
        pathBuf[i] += ".jpg";
        LOGE("%s\n",pathBuf[i].c_str());
        img_names.push_back(pathBuf[i]);
    }
    string result_name(charPath);
    result_name += "result.jpg"; 
    LOGE("%s\n",result_name.c_str());


    LOGE("TEST!\n");
    int num_images = static_cast<int>(img_names.size());
    double work_scale = 1, seam_scale = 1, compose_scale = 1;
    //特征点检测以及对图像进行预处理（尺寸缩放），然后计算每幅图形的特征点，以及特征点描述子
    LOGI("finding feature\n");
    Ptr<FeaturesFinder> finder;
    finder = new SurfFeaturesFinder();///采用Surf特征点检测 

    Mat full_img1,full_img, img;
    vector<ImageFeatures> features(num_images);
    vector<Mat> images(num_images);
    vector<Size> full_img_sizes(num_images);
    double seam_work_aspect = 1;

    for (int i = 0; i < num_images; ++i)
    {
        full_img1 = imread(img_names[i]);
        resize(full_img1,full_img, Size(mwidth,mheight));
        full_img_sizes[i] = full_img.size();

        //计算work_scale，将图像resize到面积在work_megapix*10^6以下 
        work_scale = min(1.0, sqrt(work_megapix * 1e6 / full_img.size().area()));

        resize(full_img, img, Size(), work_scale, work_scale);

        //将图像resize到面积在work_megapix*10^6以下 
        seam_scale = min(1.0, sqrt(seam_megapix * 1e6 / full_img.size().area()));
        seam_work_aspect = seam_scale / work_scale;

        // 计算图像特征点，以及计算特征点描述子，并将img_idx设置为i 
        (*finder)(img, features[i]);
        features[i].img_idx = i;
        //cout<<"Features in image #" << i+1 << ": " << features[i].keypoints.size()<<endl;
        LOGI("Features in image # %d : %d",i+1,features[i].keypoints.size());
        //将源图像resize到seam_megapix*10^6，并存入image[]中 
        resize(full_img, img, Size(), seam_scale, seam_scale);
        images[i] = img.clone();
    }

    finder->collectGarbage();
    full_img.release();
    img.release();

    //对图像进行两两匹配
    //cout<<"Pairwise matching"<<endl;
    LOGI("Pairwise matching\n");

    //使用最近邻和次近邻匹配，对任意两幅图进行特征点匹配 
    vector<MatchesInfo> pairwise_matches;
    BestOf2NearestMatcher matcher(try_gpu, match_conf);//最近邻和次近邻法
    matcher(features, pairwise_matches); //对每两个图片进行匹配
    matcher.collectGarbage();


    //将置信度高于门限的所有匹配合并到一个集合中  
    //只留下确定是来自同一全景图的图片  
    //vector<int> leaveBiggestComponent(vector<ImageFeatures> &features,  vector<MatchesInfo> &pairwise_matches,
    //float conf_threshold)
    //features表示图片特征点信息
    //pairwise_matches表示图片两两配对信息
    //conf_threshold表示图片配对置信阈值
    vector<int> indices = leaveBiggestComponent(features, pairwise_matches, conf_thresh);
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

    // 检查图片数量是否依旧满足要求
    num_images = static_cast<int>(img_names.size());
    if (num_images < 2)
    {
        LOGI("Need more images\n");
        return -1;
    }

    HomographyBasedEstimator estimator;//基于单应性的估计量
        vector<CameraParams> cameras;//相机参数
    estimator(features, pairwise_matches, cameras);

    for (size_t i = 0; i < cameras.size(); ++i)
    {
        Mat R;
        cameras[i].R.convertTo(R, CV_32F);
        cameras[i].R = R;
        LOGI("Initial intrinsics # %d\n",indices[i]+1);
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


    // 求出的焦距取中值和所有图片的焦距并构建camera参数，将矩阵写入camera
    vector<double> focals;
    for (size_t i = 0; i < cameras.size(); ++i)
    {
        LOGI("camera # %d:\n",indices[i]+1);
        focals.push_back(cameras[i].focal);
    }

    sort(focals.begin(), focals.end());
    float warped_image_scale;
    if (focals.size() % 2 == 1)
        warped_image_scale = static_cast<float>(focals[focals.size() / 2]);
    else
        warped_image_scale = static_cast<float>(focals[focals.size() / 2 - 1] + focals[focals.size() / 2]) * 0.5f;

    ///波形矫正 
    vector<Mat> rmats;
    for (size_t i = 0; i < cameras.size(); ++i)
        rmats.push_back(cameras[i].R);
    waveCorrect(rmats, wave_correct);////波形矫正 
    for (size_t i = 0; i < cameras.size(); ++i)
        cameras[i].R = rmats[i];


    //cout<<"Warping images ... "<<endl;
    LOGI("Warping images...\n");


    vector<Point> corners(num_images);//统一坐标后的顶点
    vector<Mat> masks_warped(num_images);
    vector<Mat> images_warped(num_images);
    vector<Size> sizes(num_images);
    vector<Mat> masks(num_images);//融合掩码

    // 准备图像融合掩码 
    for (int i = 0; i < num_images; ++i)
    {
        masks[i].create(images[i].size(), CV_8U);
        masks[i].setTo(Scalar::all(255));
    }

    //弯曲图像和融合掩码

    Ptr<WarperCreator> warper_creator;
    warper_creator = new cv::SphericalWarper();

    Ptr<RotationWarper> warper = warper_creator->create(static_cast<float>(warped_image_scale * seam_work_aspect));

    for (int i = 0; i < num_images; ++i)
    {
        Mat_<float> K;
        cameras[i].K().convertTo(K, CV_32F);
        float swa = (float)seam_work_aspect;
        K(0,0) *= swa; K(0,2) *= swa;
        K(1,1) *= swa; K(1,2) *= swa;

        corners[i] = warper->warp(images[i], K, cameras[i].R, INTER_LINEAR, BORDER_REFLECT, images_warped[i]);//计算统一后坐标顶点 
        sizes[i] = images_warped[i].size();

        warper->warp(masks[i], K, cameras[i].R, INTER_NEAREST, BORDER_CONSTANT, masks_warped[i]);//弯曲当前图像
    }

    vector<Mat> images_warped_f(num_images);
    for (int i = 0; i < num_images; ++i)
        images_warped[i].convertTo(images_warped_f[i], CV_32F);


    Ptr<ExposureCompensator> compensator = ExposureCompensator::createDefault(expos_comp_type);//建立补偿器以进行关照补偿，补偿方法是gain_blocks
    compensator->feed(corners, images_warped, masks_warped);

    //查找接缝 
    Ptr<SeamFinder> seam_finder;
    seam_finder = new detail::GraphCutSeamFinder(GraphCutSeamFinderBase::COST_COLOR);
    seam_finder->find(images_warped_f, corners, masks_warped);

    // 释放未使用的内存 
    images.clear();
    images_warped.clear();
    images_warped_f.clear();
    masks.clear();

    //////图像融合 
    //cout<<"Compositing..."<<endl;
    LOGI("Compositing...\n");

    Mat img_warped, img_warped_s;
    Mat dilated_mask, seam_mask, mask, mask_warped;
    Ptr<Blender> blender;

    double compose_work_aspect = 1;

    for (int img_idx = 0; img_idx < num_images; ++img_idx)
    {
        //cout<<"Compositing image #" << indices[img_idx]+1<<endl;
        LOGI("Compositing image # %d\n",indices[img_idx]+1);
        //由于以前进行处理的图片都是以work_scale进行缩放的，所以图像的内参  
        //corner（统一坐标后的顶点），mask（融合的掩码）都需要重新计算  

        // 读取图像和做必要的调整 

        full_img1 = imread(img_names[img_idx]);
        resize(full_img1,full_img, Size(mwidth,mheight));
        compose_scale = min(1.0, sqrt(compose_megapix * 1e6 / full_img.size().area()));
        compose_work_aspect = compose_scale / work_scale;
        // 更新弯曲图像比例  
        warped_image_scale *= static_cast<float>(compose_work_aspect);
        warper = warper_creator->create(warped_image_scale);

        // 更新corners和sizes
        for (int i = 0; i < num_images; ++i)
        {
            // 更新相机以下特性
            cameras[i].focal *= compose_work_aspect;
            cameras[i].ppx *= compose_work_aspect;
            cameras[i].ppy *= compose_work_aspect;

            // 更新corners和sizes
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

        if (abs(compose_scale - 1) > 1e-1)
            resize(full_img, img, Size(), compose_scale, compose_scale);
        else
            img = full_img;
        full_img.release();
        Size img_size = img.size();

        Mat K;
        cameras[img_idx].K().convertTo(K, CV_32F);
        // 扭曲当前图像 
        warper->warp(img, K, cameras[img_idx].R, INTER_LINEAR, BORDER_REFLECT, img_warped);
        // 扭曲当前图像掩模 
        mask.create(img_size, CV_8U);
        mask.setTo(Scalar::all(255));
        warper->warp(mask, K, cameras[img_idx].R, INTER_NEAREST, BORDER_CONSTANT, mask_warped);

        // 曝光补偿
        compensator->apply(img_idx, corners[img_idx], img_warped, mask_warped);

        img_warped.convertTo(img_warped_s, CV_16S);
        img_warped.release();
        img.release();
        mask.release();

        dilate(masks_warped[img_idx], dilated_mask, Mat());
        resize(dilated_mask, seam_mask, mask_warped.size());
        mask_warped = seam_mask & mask_warped;
        //初始化blender 
        if (blender.empty())
        {
            blender = Blender::createDefault(blend_type, try_gpu);
            Size dst_sz = resultRoi(corners, sizes).size();
            float blend_width = sqrt(static_cast<float>(dst_sz.area())) * blend_strength / 100.f;
            if (blend_width < 1.f)
                blender = Blender::createDefault(Blender::NO, try_gpu);
            else
            {
                MultiBandBlender* mb = dynamic_cast<MultiBandBlender*>(static_cast<Blender*>(blender));
                mb->setNumBands(static_cast<int>(ceil(log(blend_width)/log(2.)) - 1.));
                //cout<<"Multi-band blender, number of bands: " << mb->numBands()<<endl;
            }
            //根据corners顶点和图像的大小确定最终全景图的尺寸
            blender->prepare(corners, sizes);
        }

        // // 融合当前图像
        blender->feed(img_warped_s, mask_warped, corners[img_idx]);
    }

    Mat result, result_mask;
    blender->blend(result, result_mask);

    imwrite(result_name, result);

    finish=clock();
    totaltime=(double)(finish-start)/CLOCKS_PER_SEC;
    //cout<<"\nŽË³ÌÐòµÄÔËÐÐÊ±ŒäÎª"<<totaltime<<"Ãë£¡"<<endl;
    LOGI("TOTAL TIME IS %f\n",totaltime);

    //release
    vector<string>().swap(img_names);
    //system("pause");
    return 0;
}

JNIEXPORT jdouble JNICALL Java_com_example_vlcdemo_ImageProc_getTime
                (JNIEnv *env, jclass obj)
{
    return (jdouble)totaltime;
}

//JNIEXPORT jstring JNICALL Java_com_example_vlcdemo_ImageProc_getResultName
//		(JNIEnv *env, jclass obj)
//{
//	LOGI("%s\n",result_name.c_str());
//	return stoJstring(env,result_name.c_str());
//	
//}

