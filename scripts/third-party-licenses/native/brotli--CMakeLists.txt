cmake_minimum_required(VERSION 3.5)

project (brotli4j)

find_package(Java REQUIRED COMPONENTS Development)
find_package(JNI REQUIRED)

if (JNI_FOUND)
    message (STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message (STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
endif()
include_directories(${JNI_INCLUDE_DIRS})

SET (BROTLI_INCLUDE_DIRS "brotli/include" "brotli/common")
include_directories(${BROTLI_INCLUDE_DIRS})

SET (LIB_TYPE SHARED)
SET (CMAKE_JNI_TARGET TRUE)
SET (CMAKE_VERBOSE_MAKEFILE ON)
if(NOT CMAKE_BUILD_TYPE AND NOT CMAKE_CONFIGURATION_TYPES)
    SET (CMAKE_BUILD_TYPE Release)
endif()
message(STATUS "Build type is '${CMAKE_BUILD_TYPE}'")
SET (CMAKE_C_FLAGS "-flto")
SET (CMAKE_CXX_FLAGS "-Wall -flto")

SET (CMAKE_CXX_STANDARD 11)
SET (CMAKE_CXX_STANDARD_REQUIRED ON)

if(WIN32)
    SET(STATIC_LIBRARY_CXX_FLAGS /MD)
    add_compile_options(/EHsc)
    string(REPLACE "/O2" "/Ox" CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE}")
else(WIN32)
    SET(STATIC_LIBRARY_CXX_FLAGS)
endif()

add_library (brotli ${LIB_TYPE}
				"brotli/common/constants.c"
				"brotli/common/context.c"
				"brotli/common/dictionary.c"
				"brotli/common/platform.c"
				"brotli/common/shared_dictionary.c"
				"brotli/common/transform.c"
				"brotli/dec/bit_reader.c"
				"brotli/dec/decode.c"
				"brotli/dec/huffman.c"
				"brotli/dec/prefix.c"
				"brotli/dec/state.c"
				"brotli/dec/static_init.c"
				"brotli/enc/backward_references.c"
				"brotli/enc/backward_references_hq.c"
				"brotli/enc/bit_cost.c"
				"brotli/enc/block_splitter.c"
				"brotli/enc/brotli_bit_stream.c"
				"brotli/enc/cluster.c"
				"brotli/enc/command.c"
				"brotli/enc/compound_dictionary.c"
				"brotli/enc/compress_fragment.c"
				"brotli/enc/compress_fragment_two_pass.c"
				"brotli/enc/dictionary_hash.c"
				"brotli/enc/encode.c"
				"brotli/enc/encoder_dict.c"
				"brotli/enc/entropy_encode.c"
				"brotli/enc/fast_log.c"
				"brotli/enc/histogram.c"
				"brotli/enc/literal_cost.c"
				"brotli/enc/memory.c"
				"brotli/enc/metablock.c"
				"brotli/enc/static_dict.c"
				"brotli/enc/static_dict_lut.c"
				"brotli/enc/static_init.c"
				"brotli/enc/utf8_util.c"
				"brotli/tools/brotli.c"
                		"natives/src/main/cpp/common_jni.cc"
                		"natives/src/main/cpp/decoder_jni.cc"
                		"natives/src/main/cpp/encoder_jni.cc"
                )

SET_TARGET_PROPERTIES (brotli PROPERTIES LINKER_LANGUAGE CXX)
