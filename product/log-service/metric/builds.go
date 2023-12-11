// Copyright 2023 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package metric

import "github.com/prometheus/client_golang/prometheus"

// Metrics represents the metrics for the log service.
type Metrics struct {
	StreamWriteCount          prometheus.Counter
	StreamTailCount           prometheus.Counter
	StreamWriteLatency        prometheus.Histogram
	BlobUploadLatency         prometheus.Histogram
	BlobDownloadLatency       prometheus.Histogram
	BlobZipDownloadLatency    prometheus.Histogram
	BlobUploadCount           prometheus.Counter
	BlobDownloadCount         prometheus.Counter
	BlobZipDownloadCount      prometheus.Counter
	BlobUploadErrorCount      prometheus.Counter
	BlobDownloadErrorCount    prometheus.Counter
	BlobZipDownloadErrorCount prometheus.Counter
	StreamWriteErrorCount     prometheus.Counter
	StreamTailErrorCount      prometheus.Counter
}

// RegisterMetrics registers the metrics with Prometheus and returns the Metrics object.
func RegisterMetrics() *Metrics {
	metrics := &Metrics{
		// Total request count (Failed + Successful)
		StreamWriteCount: prometheus.NewCounter(
			prometheus.CounterOpts{
				Name: "log_service_stream_write_count",
				Help: "Total number of put requests to write stream ",
			},
		),
		// Total request count (Failed + Successful)
		StreamTailCount: prometheus.NewCounter(
			prometheus.CounterOpts{
				Name: "log_service_stream_tail_count",
				Help: "Total number of get requests to tail stream",
			},
		),
		StreamWriteLatency: prometheus.NewHistogram(
			prometheus.HistogramOpts{
				Name:    "log_service_stream_write_latency",
				Help:    "Latency distribution of write stream requests",
				Buckets: []float64{0, 50, 80, 110, 140, 170, 200, 230, 260, 290, 320},
			},
		),
		BlobUploadLatency: prometheus.NewHistogram(
			prometheus.HistogramOpts{
				Name:    "log_service_blob_upload_latency",
				Help:    "Latency for Blob Upload",
				Buckets: []float64{0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120},
			},
		),
		BlobDownloadLatency: prometheus.NewHistogram(
			prometheus.HistogramOpts{
				Name:    "log_service_blob_download_latency",
				Help:    "Latency for Blob download",
				Buckets: []float64{0, 0.2, 0.4, 0.6, 0.8, 1},
			},
		),
		BlobZipDownloadLatency: prometheus.NewHistogram(
			prometheus.HistogramOpts{
				Name:    "log_service_blob_zip_download_latency",
				Help:    "latency to download logs in a zip file",
				Buckets: []float64{0, 0.2, 0.4, 0.6, 0.8, 1, 2},
			},
		),
		// Total request count (Failed + Successful)
		BlobUploadCount: prometheus.NewCounter(
			prometheus.CounterOpts{
				Name: "log_service_blob_upload_count",
				Help: "Number of request for blob uploads",
			},
		),
		// Total request count (Failed + Successful)
		BlobDownloadCount: prometheus.NewCounter(
			prometheus.CounterOpts{
				Name: "log_service_blob_download_count",
				Help: "Number of requests for blob downloads",
			},
		),
		// Total request count (Failed + Successful)
		BlobZipDownloadCount: prometheus.NewCounter(
			prometheus.CounterOpts{
				Name: "log_service_blob_zip_downloads",
				Help: "Number of requests for blob zip downloads",
			},
		),
		// Total Error count for Blob upload
		BlobUploadErrorCount: prometheus.NewCounter(
			prometheus.CounterOpts{
				Name: "log_service_blob_upload_error_count",
				Help: "Number of error requests for blob uploads",
			},
		),
		// Total Error Count for Blob Download
		BlobDownloadErrorCount: prometheus.NewCounter(
			prometheus.CounterOpts{
				Name: "log_service_blob_download_error_count",
				Help: "Number of error requests for blob downloads",
			},
		),
		// Total Error Count for Blob Zip Download
		BlobZipDownloadErrorCount: prometheus.NewCounter(
			prometheus.CounterOpts{
				Name: "log_service_blob_zip_downlods_error_count",
				Help: "Number of error requests for blob zip downloads",
			},
		),
		// Total Error Count for Stream Write Error Count
		StreamWriteErrorCount: prometheus.NewCounter(
			prometheus.CounterOpts{
				Name: "log_service_stream_write_error_count",
				Help: "Number of error requests for stream write count",
			},
		),
		// Total Error Count for Stream Tail Error Count
		StreamTailErrorCount: prometheus.NewCounter(
			prometheus.CounterOpts{
				Name: "log_service_stream_tail_error_count",
				Help: "Total number of error requests to tail stream",
			},
		),
	}

	prometheus.MustRegister(metrics.StreamTailErrorCount, metrics.StreamWriteErrorCount, metrics.BlobZipDownloadErrorCount, metrics.BlobDownloadErrorCount, metrics.BlobUploadErrorCount, metrics.StreamWriteCount, metrics.StreamTailCount, metrics.StreamWriteLatency, metrics.BlobUploadLatency, metrics.BlobDownloadLatency, metrics.BlobZipDownloadLatency, metrics.BlobUploadCount, metrics.BlobDownloadCount, metrics.BlobZipDownloadCount)

	return metrics
}
