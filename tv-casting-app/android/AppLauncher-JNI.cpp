/*
 * AppLauncher-JNI.cpp
 * 
 * JNI bridge for sending ApplicationLauncher cluster commands to a commissioned Matter device.
 * Calls CastingServer::ApplicationLauncherLaunchApp() and ApplicationLauncherStopApp().
 */

#include <jni.h>
#include <lib/support/JniReferences.h>
#include <lib/support/JniTypeWrappers.h>

#include <app/server/Server.h>
#include <platform/CHIPDeviceLayer.h>
#include <CastingServer.h>
#include <TargetVideoPlayerInfo.h>
#include <ApplicationLauncher.h>

using namespace chip;

#define JNI_METHOD(RETURN, METHOD_NAME)                                                                                            \
    extern "C" JNIEXPORT RETURN JNICALL Java_com_matter_casting_AppLauncherFragment_##METHOD_NAME

JNI_METHOD(jboolean, launchApp)(JNIEnv * env, jobject thiz, jint catalogVendorId, jstring applicationId)
{
    chip::DeviceLayer::StackLock lock;
    ChipLogProgress(AppServer, "AppLauncher-JNI::launchApp called");

    // Get the active CastingPlayer (commissioned device)
    CastingServer * castingServer = CastingServer::GetInstance();
    if (castingServer == nullptr)
    {
        ChipLogError(AppServer, "CastingServer instance is null");
        return JNI_FALSE;
    }

    TargetVideoPlayerInfo * targetVideoPlayerInfo = castingServer->GetActiveTargetVideoPlayer();
    if (targetVideoPlayerInfo == nullptr || !targetVideoPlayerInfo->IsInitialized())
    {
        ChipLogError(AppServer, "No active target video player found");
        return JNI_FALSE;
    }

    // Find an endpoint that supports ApplicationLauncher cluster (Cluster ID 0x50C)
    TargetEndpointInfo * endpoints = targetVideoPlayerInfo->GetEndpoints();
    if (endpoints == nullptr)
    {
        ChipLogError(AppServer, "No endpoints available");
        return JNI_FALSE;
    }
    
    TargetEndpointInfo * appLauncherEndpoint = nullptr;
    for (size_t i = 0; i < kMaxNumberOfEndpoints && endpoints[i].IsInitialized(); i++)
    {
        if (endpoints[i].HasCluster(chip::app::Clusters::ApplicationLauncher::Id))
        {
            appLauncherEndpoint = &endpoints[i];
            ChipLogProgress(AppServer, "Found ApplicationLauncher cluster on endpoint %d", appLauncherEndpoint->GetEndpointId());
            break;
        }
    }

    if (appLauncherEndpoint == nullptr)
    {
        ChipLogError(AppServer, "No endpoint found with ApplicationLauncher cluster support");
        return JNI_FALSE;
    }

    // Convert Java string to C string
    const char * nativeApplicationId = env->GetStringUTFChars(applicationId, 0);
    
    ChipLogProgress(AppServer, "Sending ApplicationLauncher::LaunchApp command to endpoint %d: catalogVendorId=%d, appId=%s",
                    appLauncherEndpoint->GetEndpointId(), catalogVendorId, nativeApplicationId);

    // Create Application struct
    chip::app::Clusters::ApplicationLauncher::Structs::ApplicationStruct::Type application;
    application.catalogVendorID = static_cast<uint16_t>(catalogVendorId);
    application.applicationID = chip::CharSpan(nativeApplicationId, strlen(nativeApplicationId));

    // Send the ApplicationLauncher LaunchApp command
    CHIP_ERROR err = castingServer->ApplicationLauncherLaunchApp(
        appLauncherEndpoint,
        application,
        chip::NullOptional, // data (optional)
        [](CHIP_ERROR err) {
            if (err == CHIP_NO_ERROR)
            {
                ChipLogProgress(AppServer, "ApplicationLauncher::LaunchApp command succeeded");
            }
            else
            {
                ChipLogError(AppServer, "ApplicationLauncher::LaunchApp command failed: %" CHIP_ERROR_FORMAT, err.Format());
            }
        });

    env->ReleaseStringUTFChars(applicationId, nativeApplicationId);

    if (err != CHIP_NO_ERROR)
    {
        ChipLogError(AppServer, "Failed to send ApplicationLauncher::LaunchApp command: %" CHIP_ERROR_FORMAT, err.Format());
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

JNI_METHOD(jboolean, stopApp)(JNIEnv * env, jobject thiz, jint catalogVendorId, jstring applicationId)
{
    chip::DeviceLayer::StackLock lock;
    ChipLogProgress(AppServer, "AppLauncher-JNI::stopApp called");

    // Get the active CastingPlayer (commissioned device)
    CastingServer * castingServer = CastingServer::GetInstance();
    if (castingServer == nullptr)
    {
        ChipLogError(AppServer, "CastingServer instance is null");
        return JNI_FALSE;
    }

    TargetVideoPlayerInfo * targetVideoPlayerInfo = castingServer->GetActiveTargetVideoPlayer();
    if (targetVideoPlayerInfo == nullptr || !targetVideoPlayerInfo->IsInitialized())
    {
        ChipLogError(AppServer, "No active target video player found");
        return JNI_FALSE;
    }

    // Find an endpoint that supports ApplicationLauncher cluster
    TargetEndpointInfo * endpoints = targetVideoPlayerInfo->GetEndpoints();
    if (endpoints == nullptr)
    {
        ChipLogError(AppServer, "No endpoints available");
        return JNI_FALSE;
    }
    
    TargetEndpointInfo * appLauncherEndpoint = nullptr;
    for (size_t i = 0; i < kMaxNumberOfEndpoints && endpoints[i].IsInitialized(); i++)
    {
        if (endpoints[i].HasCluster(chip::app::Clusters::ApplicationLauncher::Id))
        {
            appLauncherEndpoint = &endpoints[i];
            ChipLogProgress(AppServer, "Found ApplicationLauncher cluster on endpoint %d", appLauncherEndpoint->GetEndpointId());
            break;
        }
    }

    if (appLauncherEndpoint == nullptr)
    {
        ChipLogError(AppServer, "No endpoint found with ApplicationLauncher cluster support");
        return JNI_FALSE;
    }

    // Convert Java string to C string
    const char * nativeApplicationId = env->GetStringUTFChars(applicationId, 0);
    
    ChipLogProgress(AppServer, "Sending ApplicationLauncher::StopApp command to endpoint %d: catalogVendorId=%d, appId=%s",
                    appLauncherEndpoint->GetEndpointId(), catalogVendorId, nativeApplicationId);

    // Create Application struct
    chip::app::Clusters::ApplicationLauncher::Structs::ApplicationStruct::Type application;
    application.catalogVendorID = static_cast<uint16_t>(catalogVendorId);
    application.applicationID = chip::CharSpan(nativeApplicationId, strlen(nativeApplicationId));

    // Send the ApplicationLauncher StopApp command
    CHIP_ERROR err = castingServer->ApplicationLauncherStopApp(
        appLauncherEndpoint,
        application,
        [](CHIP_ERROR err) {
            if (err == CHIP_NO_ERROR)
            {
                ChipLogProgress(AppServer, "ApplicationLauncher::StopApp command succeeded");
            }
            else
            {
                ChipLogError(AppServer, "ApplicationLauncher::StopApp command failed: %" CHIP_ERROR_FORMAT, err.Format());
            }
        });

    env->ReleaseStringUTFChars(applicationId, nativeApplicationId);

    if (err != CHIP_NO_ERROR)
    {
        ChipLogError(AppServer, "Failed to send ApplicationLauncher::StopApp command: %" CHIP_ERROR_FORMAT, err.Format());
        return JNI_FALSE;
    }

    return JNI_TRUE;
}
