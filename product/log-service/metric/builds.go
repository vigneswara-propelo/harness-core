// Copyright 2023 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package metric

import "github.com/prometheus/client_golang/prometheus"

// Metrics represents the metrics for the log service.
type Metrics struct {
	StreamWriteCount       prometheus.Counter
	StreamTailCount        prometheus.Counter
	StreamWriteLatency     prometheus.Gauge
	BlobUploadLatency      prometheus.Gauge
	BlobDownloadLatency    prometheus.Gauge
	BlobZipDownloadLatency prometheus.Gauge
	BlobUploadCount        prometheus.Counter
	BlobDownloadCount      prometheus.Counter
	BlobZipDownloadCount   prometheus.Counter
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
		StreamWriteLatency: prometheus.NewGauge(
			prometheus.GaugeOpts{
				Name: "log_service_stream_write_latency",
				Help: "Latency distribution of write stream requests",
			},
		),
		BlobUploadLatency: prometheus.NewGauge(
			prometheus.GaugeOpts{
				Name: "log_service_blob_upload_latency",
				Help: "Latency for Blob Upload",
			},
		),
		BlobDownloadLatency: prometheus.NewGauge(
			prometheus.GaugeOpts{
				Name: "log_service_blob_download_latency",
				Help: "Latency for Blob download",
			},
		),
		BlobZipDownloadLatency: prometheus.NewGauge(
			prometheus.GaugeOpts{
				Name: "log_service_blob_zip_download_latency",
				Help: "latency to download logs in a zip file",
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
	}

	prometheus.MustRegister(metrics.StreamWriteCount, metrics.StreamTailCount, metrics.StreamWriteLatency, metrics.BlobUploadLatency, metrics.BlobDownloadLatency, metrics.BlobZipDownloadLatency, metrics.BlobUploadCount, metrics.BlobDownloadCount, metrics.BlobZipDownloadCount)

	return metrics
}
